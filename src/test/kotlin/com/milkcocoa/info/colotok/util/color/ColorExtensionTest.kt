package com.milkcocoa.info.colotok.util.color

import com.milkcocoa.info.colotok.util.color.ColorExtension.black
import com.milkcocoa.info.colotok.util.color.ColorExtension.blue
import com.milkcocoa.info.colotok.util.color.ColorExtension.cyan
import com.milkcocoa.info.colotok.util.color.ColorExtension.green
import com.milkcocoa.info.colotok.util.color.ColorExtension.magenta
import com.milkcocoa.info.colotok.util.color.ColorExtension.red
import com.milkcocoa.info.colotok.util.color.ColorExtension.white
import com.milkcocoa.info.colotok.util.color.ColorExtension.yellow
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

object ColorExtensionTest {
    @Test
    fun colorExtensionTest() {
        Assertions.assertTrue {
            "black".black().equals(
                "\u001B[30mblack\u001B[0m"
            )
        }
        Assertions.assertTrue {
            "red".red().equals(
                "\u001B[31mred\u001B[0m"
            )
        }

        Assertions.assertTrue {
            "green".green().equals(
                "\u001B[32mgreen\u001B[0m"
            )
        }

        Assertions.assertTrue {
            "yellow".yellow().equals(
                "\u001B[33myellow\u001B[0m"
            )
        }

        Assertions.assertTrue {
            "blue".blue().equals(
                "\u001B[34mblue\u001B[0m"
            )
        }

        Assertions.assertTrue {
            "magenta".magenta().equals(
                "\u001B[35mmagenta\u001B[0m"
            )
        }

        Assertions.assertTrue {
            "cyan".cyan().equals(
                "\u001B[36mcyan\u001B[0m"
            )
        }

        Assertions.assertTrue {
            "white".white().equals(
                "\u001B[37mwhite\u001B[0m"
            )
        }
    }
}