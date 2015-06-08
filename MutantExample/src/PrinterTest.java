import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;


public class PrinterTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Test
	/**
	 * Tests the method to get all professor names
	 * @InputModel "http://company" Company theCompany = 
	 
            +----------------------+
            | theCompany : Company |-----------{employees}---[e]
            +----------------------+
             |             
             |{departments}
             |
             v
         +-------------------+
         | : Department      |
         |-------------------|
         | budget=25000000   |
         | name="Accounting" |-----------------.
         +-------------------+                 |{employees}
                                               v
                  +-----------------+  +----------------+
         [e]----->| : Employee      |  | : Employee     |<----[e]
                  |-----------------|  |----------------|
                  | name = "Alice"  |  | name = "Bob"   |
                  +-----------------+  +----------------+
	 *       
	 */
	public void testGetAllEmployeeNames() {
		fail("Not yet implemented");
	}
	

}
