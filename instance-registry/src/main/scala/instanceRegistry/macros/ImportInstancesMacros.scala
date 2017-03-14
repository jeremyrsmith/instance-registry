package instanceRegistry.macros

import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

import scala.collection.JavaConverters._
import scala.collection.immutable.Queue
import scala.reflect.macros.whitebox

class ImportInstancesMacros(val c: whitebox.Context) {
  import c.universe._

  private def check(str: String) = try {
    Right(c.typecheck(c.parse(str)))
  } catch {
    case err: Throwable =>
      Left(err.getMessage)
  }

  private def validate(tree: Tree, typ: Type) = if(tree.tpe <:< typ)
    Right(tree)
  else
    Left(s"$tree is not of type $typ")

  private def findExportedInstances(T: Type, params: Type*) = {

    val friendly = s"$T[${params.mkString(", ")}]"

    println(s"Finding $friendly")

    val err = s"No instance for $friendly found in compile dependencies"

    val argStrings = params.toList.map(_.typeSymbol.fullName)
    val targetType = appliedType(T, params: _*)

    val result = Instances.find(T, params.toList)

    result.getOrElse {
      c.abort(c.enclosingPosition, err)
    }
  }

  def findExported[T : WeakTypeTag, A : WeakTypeTag]: Tree =
    findExportedInstances(weakTypeOf[T].dealias, weakTypeOf[A].dealias)

  def findExported2[T : WeakTypeTag, A : WeakTypeTag, B : WeakTypeTag]: Tree =
    findExportedInstances(
      weakTypeOf[T].dealias,
      weakTypeOf[A].dealias,
      weakTypeOf[B].dealias
    )

  private def readItems(url: URL) = url.getProtocol match {
    case "file" =>
      new File(url.getPath.replace('/', File.separatorChar)).listFiles().toList.flatMap {
        file =>
          val source = scala.io.Source.fromFile(file)
          val lines = source.getLines().toList
          source.close()
          lines
      }
    case "jar" =>
      val path = new URL(url.getPath).getPath
      val (jarFile, jarPath) = path.splitAt(path.indexOf(".jar!") + 5)
      val jar = new JarFile(new File(jarFile.stripSuffix("!").replace('/', File.separatorChar)))
      val normalizedJarPath = jarPath.stripPrefix("/")
      jar.entries().asScala.filter {
        entry =>
          val name = entry.getName.stripPrefix("/").stripSuffix("/")
          (name startsWith normalizedJarPath) && name != normalizedJarPath
      }.toList.flatMap {
        entry =>
          val source = scala.io.Source.fromInputStream(jar.getInputStream(entry))
          val lines = source.getLines().toList
          source.close()
          lines
      }
    case other => Nil
  }

  private object Instances {
    // TODO: a path-like trie structure would be better here
    val cached = new ConcurrentHashMap[(String, List[String]), Tree]()

    private def isJarUrl(url: URL) = url.getPath

    def find(T: Type, params: List[Type]): Option[Tree] = {
      val typeclass = T.typeSymbol.fullName
      val args = params.map(_.typeSymbol.fullName)

      def load() = {
        val location = s"META-INF/exported-instances/$typeclass/" + args.mkString("/")
        val resources =
          getClass.getClassLoader.getResources(location).asScala.toList

        try {
          val items = resources.flatMap(readItems)

          val attempts = items.toStream.map {
            accessor => for {
              tree  <- check(accessor).right
              valid <- validate(tree, appliedType(T, params: _*)).right
            } yield valid
          }

          val result = attempts.collectFirst {
            case Right(tree) => tree
          }

          result foreach {
            item => cached.putIfAbsent((typeclass, args), item)
          }

          result

        } catch {
          case err: Throwable =>
            val e = err
            println(e)
            None
        }
      }

      Option(cached.get((typeclass, args))) orElse load()
    }
  }
}
