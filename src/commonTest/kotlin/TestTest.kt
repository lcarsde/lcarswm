import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestTest {
    @BeforeTest
    fun setup() {
        println("this is the common setup method")
    }

    @AfterTest
    fun tearDown() {
        println("this is the common tearDown method")
    }

    @Test
    fun `my test`() {
        println("this is my common test")
    }
}