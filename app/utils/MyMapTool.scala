package utils

import play.api.libs.json.Json

trait MyMapTool {
  implicit class MyMap(map: Map[String,String]) {
    def mapToJson: String = {
      Json.toJson(map).toString()
    }
  }
}
