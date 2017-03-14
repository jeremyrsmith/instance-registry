package instanceRegistry.export.macros

import java.io.File

import scala.reflect.macros.whitebox

class ExportMacros(val c: whitebox.Context) {
  import c.universe._

  // figure out where to put the files
  // I'm not sure how reliable this is. Maybe it's a hint that this is a bad place to do this.
  private val resourcesDir = {
    val file = c.enclosingUnit.source.path
    c.compilerSettings.dropWhile(_ != "-classpath").tail.headOption.toList
      .flatMap {
        cp => cp.split(File.pathSeparatorChar).toList
      }
      .sortBy {
        str => str.zip(file).takeWhile(t => t._1 == t._2).length
      }
      .lastOption
      .map(new File(_))
      .map(root => new File(root, ("META-INF" :: "exported-instances" :: Nil).mkString(File.separator)))
      .getOrElse {
        c.abort(c.enclosingPosition, "Can't determine where to output the instance manifests")
      }
  }

  def stableOwner(owner: Symbol): Tree = owner match {
    case root if root.owner.owner == NoSymbol => Ident(root.name)
    case mod if mod.isModule  => Select(stableOwner(mod.owner), mod.name.toTermName)
    case pkg if pkg.isPackage => Select(stableOwner(pkg.owner), pkg.name.toTermName)
    case _ => c.abort(c.enclosingPosition, "No stable accessor")
  }

  def instance(annottees: Tree*): Tree = {
    val target = annottees.toList match {
      case (v @ ValDef(_, _, _, _)) :: Nil => v
      case (m @ ModuleDef(_, _, _)) :: Nil => m
      case other :: _ => c.abort(other.pos, s"@instance can only be applied to stable values and objects")
      case Nil => c.abort(c.enclosingPosition, "Missing annottee for @instance")
    }

    val ownerSym = c.enclosingClass.symbol
    val owner = stableOwner(ownerSym)
    val ref = Select(owner, target.name)
    val out = showCode(ref)

    val targetType = try {
      target match {
        case ValDef(mods, name, tpt, rhs) =>
          val t = c.typecheck(q"(null: $tpt)")
          t.tpe
        case ModuleDef(mods, name, template) =>
          val tpt = template.parents.head
          val t = c.typecheck(q"(null: $tpt)")
          t.tpe
      }
    } catch {
      case err: Throwable =>
        println(err)
        val e = err
        c.abort(c.enclosingPosition, "Could not determine type of exported instance")
    }

    val path = new File(
      resourcesDir,
      (targetType.typeSymbol.fullName :: targetType.typeArgs.map(_.typeSymbol.fullName)) mkString File.separator
    )

    path.mkdirs()

    val outFile = new File(
      path,
      out
    )

    outFile.createNewFile()

    target
  }
}
