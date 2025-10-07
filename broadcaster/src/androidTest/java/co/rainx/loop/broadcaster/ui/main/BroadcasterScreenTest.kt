package co.rainx.loop.broadcaster.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.rainx.loop.broadcaster.data.CommandData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BroadcasterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenScreenIsLaunched_showsInitialContent() {
        composeTestRule.setContent {
            BroadcasterScreen(
                running = false,
                label = "—",
                commandCount = 0,
                commands = emptyList(),
                onStart = {},
                onStop = {}
            )
        }

        with(composeTestRule) {
            onNodeWithText("loop broadcaster").assertIsDisplayed()
            onNodeWithText("Advertises _loop._tcp. and serves /info, /ping, /command over HTTP").assertIsDisplayed()
            onNodeWithText("Start").assertIsEnabled()
            onNodeWithText("Stop").assertIsNotEnabled()
            onNodeWithText("Service: —").assertIsDisplayed()
            onNodeWithText("Commands handled: 0").assertIsDisplayed()
        }
    }

    @Test
    fun whenServiceIsRunning_showsRunningState() {
        composeTestRule.setContent {
            BroadcasterScreen(
                running = true,
                label = "MyTestDevice",
                commandCount = 0,
                commands = emptyList(),
                onStart = {},
                onStop = {}
            )
        }

        with(composeTestRule) {
            onNodeWithText("Start").assertIsNotEnabled()
            onNodeWithText("Stop").assertIsEnabled()
            onNodeWithText("Service: MyTestDevice").assertIsDisplayed()
        }
    }

    @Test
    fun startButton_callsOnStart() {
        var onStartCalled = false
        composeTestRule.setContent {
            BroadcasterScreen(
                running = false,
                label = "—",
                commandCount = 0,
                commands = emptyList(),
                onStart = { onStartCalled = true },
                onStop = {}
            )
        }

        composeTestRule.onNodeWithText("Start").performClick()
        assert(onStartCalled)
    }

    @Test
    fun stopButton_callsOnStop() {
        var onStopCalled = false
        composeTestRule.setContent {
            BroadcasterScreen(
                running = true,
                label = "MyTestDevice",
                commandCount = 0,
                commands = emptyList(),
                onStart = {},
                onStop = { onStopCalled = true }
            )
        }

        composeTestRule.onNodeWithText("Stop").performClick()
        assert(onStopCalled)
    }

    @Test
    fun whenCommandsAreReceived_theyAreDisplayed() {
        val commands = listOf(
            CommandData(
                type = "test_type_1",
                delta = 123,
                fromIp = "192.168.1.1",
                timestamp = "12:00:00"
            ),
            CommandData(
                type = "test_type_2",
                delta = 456,
                fromIp = "192.168.1.2",
                timestamp = "12:00:01"
            )
        )
        composeTestRule.setContent {
            BroadcasterScreen(
                running = true,
                label = "MyTestDevice",
                commandCount = 2,
                commands = commands,
                onStart = {},
                onStop = {}
            )
        }

        with(composeTestRule) {
            onNodeWithText("Commands handled: 2").assertIsDisplayed()
            onNodeWithText("type: test_type_1, delta: 123, from: 192.168.1.1 at 12:00:00")
                .assertIsDisplayed()
            onNodeWithText("type: test_type_2, delta: 456, from: 192.168.1.2 at 12:00:01")
                .assertIsDisplayed()
        }
    }
}
