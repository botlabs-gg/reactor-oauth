package gg.botlabs.oauth

import net.wussmann.kenneth.mockfuel.junit.MockFuelExtension
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockFuelExtension::class)
class Tests {

    private val app = OAuthApplication(
        "",
        "id",
        "secret",
        "http://redirect.url"
    )

}