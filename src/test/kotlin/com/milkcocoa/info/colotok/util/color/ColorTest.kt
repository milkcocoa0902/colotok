package com.milkcocoa.info.colotok.util.color

import com.milkcocoa.info.colotok.util.color.std.StdIn
import com.milkcocoa.info.colotok.util.color.std.StdOut
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

object ColorTest {
    @BeforeEach
    public fun before(){
        System.setIn(StdIn())
        System.setOut(StdOut())
    }

    @AfterEach
    public fun after(){
        System.setIn(null)
        System.setOut(null)
    }

    @Test
    fun foregroundTest(){
        Assertions.assertTrue {
            Color.foreground("black", AnsiColor.BLACK).equals(
                "\u001B[30mblack\u001B[0m"
            )
        }
        Assertions.assertTrue {
            Color.foreground("red", AnsiColor.RED).equals(
                "\u001B[31mred\u001B[0m"
            )
        }

        Assertions.assertTrue {
            Color.foreground("green", AnsiColor.GREEN).equals(
                "\u001B[32mgreen\u001B[0m"
            )
        }

        Assertions.assertTrue {
            Color.foreground("yellow", AnsiColor.YELLOW).equals(
                "\u001B[33myellow\u001B[0m"
            )
        }

        Assertions.assertTrue {
            Color.foreground("blue", AnsiColor.BLUE).equals(
                "\u001B[34mblue\u001B[0m"
            )
        }

        Assertions.assertTrue {
            Color.foreground("magenta", AnsiColor.MAGENTA).equals(
                "\u001B[35mmagenta\u001B[0m"
            )
        }

        Assertions.assertTrue {
            Color.foreground("cyan", AnsiColor.CYAN).equals(
                "\u001B[36mcyan\u001B[0m"
            )
        }

        Assertions.assertTrue {
            Color.foreground("white", AnsiColor.WHITE).equals(
                "\u001B[37mwhite\u001B[0m"
            )
        }
    }
}