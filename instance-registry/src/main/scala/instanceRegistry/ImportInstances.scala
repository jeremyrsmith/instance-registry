package instanceRegistry

import instanceRegistry.macros.ImportInstancesMacros

object ImportInstances {

  // TODO: these could be mechanically generated
  trait `F[_]`[Typeclass[_[_]]] {
    implicit def imported$inst[F[_]]: Typeclass[F] = macro ImportInstancesMacros.findExported[Typeclass[F], F[_]]
  }

  trait `F[_, _]`[Typeclass[_[_, _]]] {
    implicit def imported$inst[F[_, _]]: Typeclass[F] = macro ImportInstancesMacros.findExported[Typeclass[F], F[_, _]]
  }

  trait `F[_[_]]`[Typeclass[_[_[_]]]] {
    implicit def imported$inst[F[_[_]]]: Typeclass[F] = macro ImportInstancesMacros.findExported[Typeclass[F], F[A] forSome { type A[_] }]
  }

  trait `F[_[_], _[_]]`[Typeclass[_[_[_], _[_]]]] {
    implicit def imported$inst[F[_[_], _[_]]]: Typeclass[F] = macro ImportInstancesMacros.findExported[Typeclass[F], F[A, B] forSome { type A[_]; type B[_] }]
  }

  trait `F[_], G[_]`[Typeclass[_[_], _[_]]] {
    implicit def imported$inst[F[_], G[_]]: Typeclass[F, G] = macro ImportInstancesMacros.findExported2[Typeclass[F, G], F[_], G[_]]
  }

  trait `F[_[_]], G[_[_]]`[Typeclass[_[_[_]], _[_[_]]]] {
    implicit def imported$inst[F[_[_]], G[_[_]]]: Typeclass[F, G] = macro ImportInstancesMacros.findExported2[Typeclass[F, G], F[A] forSome { type A[_] }, F[G] forSome { type G[_] }]
  }

}



