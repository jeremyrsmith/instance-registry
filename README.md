# instance-registry

## Import-free inter-module typeclass instances

This project is a proof-of-concept for *import-free* typeclass instances in Scala. It's similar in goals to
[export-hook](https://github.com/milessabin/export-hook), but it is a bit different in approach (in ways described
below).

### Orphan typeclass instances

If you're writing a typeclass `Foo[F[_]]`, you can put instances for common Scala types like `List`, `Option`, etc.,
in the companion object `Foo`. That way, nobody has to import these instances in order to use them. They're in *implicit
scope*. You put your typeclass in a great library called `foo`.

Imagine someone is designing another library, with a data structure `FancyList[T]`. If they're cool with declaring a
dependency on `foo`, they could add a typeclass instance `Foo[FancyList]` to the companion object `FancyList`. In that
case, we're still cool - no imports necessary (beyond `Foo` and `FancyList`), as the companion object `FancyList` is in
implicit scope for `Foo[FancyList]`.

But there's another case that present a problem, and (I believe) harm adoption of typeclass-based design among
beginners.

First, imagine that the person who created the `fancylist` library didn't know about `Foo`. Or maybe they didn't feel
comfortable declaring a dependency on it in their core module. They (or a third party) *could* make a "glue module"
that has a dependency on both `foo` and `fancylist`, and has an instance for `Foo[FancyList]`. But because it cannot
be in either companion object `Foo` or `FancyList`, that instance will always have to be explicitly imported somehow.
This can become very confusing when many typeclasses and data structures are involved.

### What instance-registry does

Instance registry contains two pieces: a tiny, zero-dependency library for typeclass authors, and a compiler plugin for
instance authors.

The library allows the following when defining a typeclass:

```scala
package foo
import instanceRegistry.ImportInstances

abstract class Foo[F[_]] {
  def apply[A](fa: F[A]): String
}

object Foo extends ImportInstances.`F[_]`[Foo] {
  implicit val list: Foo[List] = ???
  implicit val option: Foo[Option] = ???
  // etc
} 
```

By having the companion object extend the correct shape of `ImportInstances`, we've brought in the functionality that
will enable others to create zero-import instances - because it contains a macro which does the following (at *compile
time only!*):

When an instance of `foo.Foo[fancylist.FancyList]` is needed, but is not defined in any higher priority, the search
mechanism in`ImportInstances` takes over as a last resort. It asks the compiler's classloader to look for the directory
`META-INF/exported-instances/foo.Foo/fancylist.FancyList` in the compile classpath. If the directory exists, the files
within are enumerated. Each file contains a stable Scala expression that refers to a value of type
`foo.Foo[fancylist.FancyList]`. It takes the first one which successfully typechecks as such, caches the lookup, and
returns the instance.

Please note that this happens at compile time, and there is no runtime impact (other than the presence of these
metadata files in the JAR, which are tiny).

Of course, if we're defining the instances, we don't want to have to add a bunch of these files to our project. That's
where the compiler plugin comes in. Add it to your project with one line in SBT, and annotate each instance:

```scala
package fancylist.instances

import instanceRegistry.export.instance
import foo.Foo
import fancylist.FancyList

@instance object FooInstance extends Foo[FancyList] {
  def apply[A](fa: FancyList[A]): String = ???
} 
```

The compiler plugin generates the necessary metadata files into your target directory, where they'll be integrated into
the resulting JAR.

Now anyone who simply has a dependency on that JAR will be able to use the `Foo[FancyList]` instance with *zero imports*.

### Feedback

This is a proof-of-concept. Maybe it's a terrible idea, or maybe it would be useful. I think the more we can do to
make typeclasses seamless to use, the better adoption they (and FP overall) will enjoy. Maybe export-hook is sufficient
for this (it allows better control over priority, for example), but in my opinion, the experience provided here (if
sufficiently polished) would allow for nicer code, less import clutter, and less frustration of beginners trying to find
what to import, accidentally importing the same instance twice (leading to implicit divergence), etc.

With this implemented, we could even have a search tool that indexes instances from artifacts in Maven Central which
declare a dependency on `instance-registry`. Wouldn't it be great to just search for the package that will get you the
instance you need, and once you have it, you don't even have to import anything? I sure think so!

Drop me a line on gitter (or here on GitHub) and let me know what you think!