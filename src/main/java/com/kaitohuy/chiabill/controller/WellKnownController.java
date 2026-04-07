package com.kaitohuy.chiabill.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WellKnownController {

    @GetMapping(value = "/.well-known/assetlinks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAssetLinks() {
        return """
                [
                  {
                    "relation": [
                      "delegate_permission/common.handle_all_urls"
                    ],
                    "target": {
                      "namespace": "android_app",
                      "package_name": "com.kaitohuy.chiabill",
                      "sha256_cert_fingerprints": [
                        "FB:E3:99:0C:E9:0D:A6:17:18:AB:DA:16:2E:71:13:87:3D:A6:7F:97:EA:45:6A:78:50:2E:A4:61:13:DF:A4:97"
                      ]
                    }
                  }
                ]
                """;
    }
}
