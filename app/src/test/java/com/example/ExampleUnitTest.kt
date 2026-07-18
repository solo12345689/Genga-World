package com.example

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.URL

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun downloadFiles() {
    try {
      val serverUrl = "https://raw.githubusercontent.com/solo12345689/moviebox-internal-api/main/moviebox_api_server.py"
      val serverContent = URL(serverUrl).readText()
      File("../api_server_download.txt").writeText(serverContent)
      println("Server downloaded successfully!")

      val dashboardUrl = "https://raw.githubusercontent.com/solo12345689/moviebox-internal-api/main/dashboard/app/page.tsx"
      val dashboardContent = URL(dashboardUrl).readText()
      File("../dashboard_download.txt").writeText(dashboardContent)
      println("Dashboard downloaded successfully!")

      val docUrl = "https://raw.githubusercontent.com/solo12345689/moviebox-internal-api/main/OFFICIAL_API_DOCUMENTATION.md"
      val docContent = URL(docUrl).readText()
      File("../api_doc.txt").writeText(docContent)
      println("Doc downloaded successfully!")
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
