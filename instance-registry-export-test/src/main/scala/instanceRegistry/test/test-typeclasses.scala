package instanceRegistry.test

import instanceRegistry.ImportInstances

abstract class TestTypeclass1[F[_]]
object TestTypeclass1 extends ImportInstances.`F[_]`[TestTypeclass1]
abstract class TestTypeclass2[F[_[_]]]
abstract class TestTypeclass3[F[_, _]]
abstract class TestTypeclass4[F[_[_], _[_]]]
abstract class TestTypeclass5[F[_], G[_]]
abstract class TestTypeclass6[F[_[_]], G[_[_]]]
