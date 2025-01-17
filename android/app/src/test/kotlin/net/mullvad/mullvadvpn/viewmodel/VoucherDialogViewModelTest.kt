package net.mullvad.mullvadvpn.viewmodel

import android.content.res.Resources
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.mullvad.mullvadvpn.compose.state.VoucherDialogState
import net.mullvad.mullvadvpn.lib.common.test.TestCoroutineRule
import net.mullvad.mullvadvpn.model.VoucherSubmission
import net.mullvad.mullvadvpn.model.VoucherSubmissionError
import net.mullvad.mullvadvpn.model.VoucherSubmissionResult
import net.mullvad.mullvadvpn.ui.serviceconnection.ServiceConnectionContainer
import net.mullvad.mullvadvpn.ui.serviceconnection.ServiceConnectionManager
import net.mullvad.mullvadvpn.ui.serviceconnection.ServiceConnectionState
import net.mullvad.mullvadvpn.ui.serviceconnection.VoucherRedeemer
import net.mullvad.mullvadvpn.ui.serviceconnection.voucherRedeemer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VoucherDialogViewModelTest {
    @get:Rule val testCoroutineRule = TestCoroutineRule()

    private val mockServiceConnectionManager: ServiceConnectionManager = mockk()
    private val mockServiceConnectionContainer: ServiceConnectionContainer = mockk()
    private val mockVoucherSubmission: VoucherSubmission = mockk()
    private val serviceConnectionState =
        MutableStateFlow<ServiceConnectionState>(ServiceConnectionState.Disconnected)

    private val mockVoucherRedeemer: VoucherRedeemer = mockk()
    private val mockResources: Resources = mockk()

    private lateinit var viewModel: VoucherDialogViewModel

    @Before
    fun setUp() {
        every { mockServiceConnectionManager.connectionState } returns serviceConnectionState

        viewModel =
            VoucherDialogViewModel(
                serviceConnectionManager = mockServiceConnectionManager,
                resources = mockResources
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testSubmitVoucher() = runTest {
        val voucher = DUMMY_INVALID_VOUCHER
        val dummyStringResource = DUMMY_STRING_RESOURCE

        // Arrange
        every { mockServiceConnectionManager.voucherRedeemer() } returns mockVoucherRedeemer
        every { mockVoucherSubmission.timeAdded } returns 0
        coEvery { mockVoucherRedeemer.submit(voucher) } returns
            VoucherSubmissionResult.Ok(mockVoucherSubmission)

        // Act
        assertIs<VoucherDialogState.Default>(viewModel.uiState.value.voucherViewModelState)
        viewModel.onRedeem(voucher)

        // Assert
        coVerify(exactly = 1) { mockVoucherRedeemer.submit(voucher) }
    }

    @Test
    fun testInsertInvalidVoucher() = runTest {
        val voucher = DUMMY_INVALID_VOUCHER
        val dummyStringResource = DUMMY_STRING_RESOURCE

        // Arrange
        every { mockServiceConnectionManager.voucherRedeemer() } returns mockVoucherRedeemer
        every { mockResources.getString(any()) } returns dummyStringResource
        every { mockVoucherSubmission.timeAdded } returns 0
        coEvery { mockVoucherRedeemer.submit(voucher) } returns
            VoucherSubmissionResult.Error(VoucherSubmissionError.OtherError)

        // Act, Assert
        viewModel.uiState.test {
            assertEquals(viewModel.uiState.value, awaitItem())
            serviceConnectionState.value =
                ServiceConnectionState.ConnectedReady(mockServiceConnectionContainer)
            viewModel.onRedeem(voucher)
            assertTrue { awaitItem().voucherViewModelState is VoucherDialogState.Verifying }
            assertTrue { awaitItem().voucherViewModelState is VoucherDialogState.Error }
        }
    }

    @Test
    fun testInsertValidVoucher() = runTest {
        val voucher = DUMMY_VALID_VOUCHER
        val dummyStringResource = DUMMY_STRING_RESOURCE

        // Arrange
        every { mockServiceConnectionManager.voucherRedeemer() } returns mockVoucherRedeemer
        every { mockResources.getString(any()) } returns dummyStringResource
        every { mockVoucherSubmission.timeAdded } returns 0
        coEvery { mockVoucherRedeemer.submit(voucher) } returns
            VoucherSubmissionResult.Ok(VoucherSubmission(0, DUMMY_STRING_RESOURCE))

        // Act, Assert
        viewModel.uiState.test {
            assertEquals(viewModel.uiState.value, awaitItem())
            serviceConnectionState.value =
                ServiceConnectionState.ConnectedReady(mockServiceConnectionContainer)
            viewModel.onRedeem(voucher)
            assertTrue { awaitItem().voucherViewModelState is VoucherDialogState.Verifying }
            assertTrue { awaitItem().voucherViewModelState is VoucherDialogState.Success }
        }
    }

    companion object {
        private const val DUMMY_VALID_VOUCHER = "dummy_valid_voucher"
        private const val DUMMY_INVALID_VOUCHER = "dummy_invalid_voucher"
        private const val DUMMY_STRING_RESOURCE = "dummy_string_resource"
    }
}
