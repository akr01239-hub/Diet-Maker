package com.nutriai.ui.health

import app.cash.turbine.test
import com.nutriai.data.repository.HealthRepository
import com.nutriai.domain.model.HealthStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits Connected when repository succeeds`() = runTest(dispatcher) {
        val repo = mockk<HealthRepository>()
        val status = HealthStatus(service = "nutriai-api", version = "0.1.0", aiProvider = "rules")
        coEvery { repo.check() } returns status

        val vm = HealthViewModel(repo)

        vm.uiState.test {
            assertEquals(HealthUiState.Loading, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(HealthUiState.Connected(status), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error when repository throws`() = runTest(dispatcher) {
        val repo = mockk<HealthRepository>()
        coEvery { repo.check() } throws RuntimeException("boom")

        val vm = HealthViewModel(repo)

        vm.uiState.test {
            assertEquals(HealthUiState.Loading, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is HealthUiState.Error)
            assertEquals("boom", (state as HealthUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
