package instanceRegistry.export.plugin

import java.io.{File, FileOutputStream, OutputStreamWriter}
import java.security.MessageDigest

import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.tools.nsc.plugins.{PluginComponent, Plugin => ScalaPlugin}
import scala.tools.nsc.{Global, Phase}

class Plugin(val global: Global) extends ScalaPlugin {
  val name = "instance-registry-export"
  val description = "Export typeclass instances"
  val components = new TypeclassExporter(this, global) :: Nil
}

class TypeclassExporter(plugin: ScalaPlugin, val global: Global) extends PluginComponent {
  import global._
  val runsAfter = List("typer")
  override val runsBefore: List[String] = List("patmat")
  val phaseName = "export-typeclass-instances"

  def newPhase(prev: Phase): Phase = new ExportPhase(prev)

  class ExportPhase(prev: Phase) extends StdPhase(prev) {
    def apply(unit: global.CompilationUnit): Unit = {
      val targetDir = global.currentSettings.outputDirs.outputDirFor(unit.source.file).canonicalPath
      val exportedSymbols = check(Queue(unit.body), Queue.empty).toList.distinct

      val exportTypes = exportedSymbols.map {
        sym =>
          val nameStr = sym.fullName
          val symType = sym.typeSignature.dealiasWiden
          val typ = symType.baseTypeSeq.toList.dropWhile(_.typeArgs.isEmpty).headOption
            .getOrElse {
              global.warning(
                sym.pos,
                s"Can't find any type of $sym that appears to be a typeclass; " +
                  s"exporting as $symType (which may not be what you want)"
              )
              symType
            }
          (nameStr, typ)
      }

      exportTypes foreach {
        case (accessor, typ) =>
          val path = (typ.typeConstructor :: typ.typeArgs).map(_.typeSymbol.fullNameString)
          val outDir = new File(
            new File(targetDir),
            ("META-INF" :: "exported-instances" :: path).mkString(File.separator)
          )
          outDir.mkdirs()

          // the resulting tree is placed inside a file rather than just making an empty file with it as the name
          // in case the tree contains characters that aren't filesystem-safe. Instead, the tree is given a unique
          // identifier based on the SHA-1 hash.
          val hash = MessageDigest.getInstance("SHA-1").digest(accessor.getBytes("UTF-8"))
          val outFile = new File(
            outDir,
            javax.xml.bind.DatatypeConverter.printHexBinary(hash)
          )

          if(outFile.createNewFile()) {
            val writer = new OutputStreamWriter(new FileOutputStream(outFile))
            writer.write(accessor)
            writer.close()
          }
      }
    }

    @tailrec private def check(trees: Queue[Tree], annotated: Queue[Symbol]): Queue[Symbol] = trees.headOption match {
      case Some(tree) if tree.symbol != null =>
        tree.symbol.getAnnotation(symbolOf[instanceRegistry.export.instance]) match {
          case Some(annotationInfo) if tree.symbol.isPublic && tree.symbol.isStable =>
            check(trees.tail enqueue tree.children, annotated enqueue tree.symbol)
          case Some(annotationInfo) if tree.symbol.hasGetter =>
            check(trees.tail enqueue tree.children, annotated.enqueue(tree.symbol.getterIn(tree.symbol.owner)))
          case Some(annotationInfo) =>
            val sym = tree.symbol
            check(trees.tail enqueue tree.children, annotated)
          case _ =>
            check(trees.tail enqueue tree.children, annotated)
        }
      case Some(tree @ ValDef(_, _, _, _)) =>
        check(trees.tail enqueue tree.children, annotated)
      case Some(tree) =>
        check(trees.tail enqueue tree.children, annotated)
      case None => annotated
    }
  }
}
