package instanceRegistry.test

import instanceRegistry.export.instance

object Instances {

  @instance object Example1 extends TestTypeclass1[List]

  @instance val example2: TestTypeclass1[Option] = new TestTypeclass1[Option] {

  }
}
