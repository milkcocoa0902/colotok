package quality.consumers.loki

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun publishedLoggerDeliversMessage() =
        runTest {
            assertEquals(listOf("published loki works"), smoke())
        }
}