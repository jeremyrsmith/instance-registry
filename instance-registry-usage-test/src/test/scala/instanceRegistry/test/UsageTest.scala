package instanceRegistry.test

import org.scalatest.FlatSpec

class UsageTest extends FlatSpec {

  "Instances" should "be found" in {

    implicitly[TestTypeclass1[List]]
    implicitly[TestTypeclass1[Option]]

  }

}
