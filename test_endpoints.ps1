# PowerShell script to test all core Smart Milk Delivery API endpoints

$headers = @{
    "Content-Type" = "application/json; charset=utf-8"
}

function Invoke-RestWithDetails {
    param(
        [string]$Uri,
        [string]$Method,
        [string]$Body,
        [hashtable]$Headers
    )
    try {
        if ($Body) {
            $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($Body)
            $response = Invoke-RestMethod -Uri $Uri -Method $Method -Body $bodyBytes -Headers $Headers
        } else {
            $response = Invoke-RestMethod -Uri $Uri -Method $Method -Headers $Headers
        }
        return $response
    } catch {
        Write-Host "HTTP Request failed!" -ForegroundColor Red
        if ($_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $responseBody = $reader.ReadToEnd()
                Write-Host "Server response body: $responseBody" -ForegroundColor Yellow
            }
        } else {
            Write-Host "No HTTP response found. Details: $_" -ForegroundColor Red
        }
        throw $_
    }
}

Write-Output "=========================================================="
Write-Output "STARTING FULL SMART MILK DELIVERY API VERIFICATION TESTS"
Write-Output "=========================================================="

# 0. Resetting Database
Write-Output "`n--- 0. Resetting Database ---"
try {
    $resetResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/auth/reset-db" -Method Post -Headers $headers
    Write-Output "Response: $resetResponse"
} catch {
    # Handled inside function
}

# 1. Register User (Milkman - Unmapped Initially)
Write-Output "`n--- 1. Registering User (Milkman) ---"
$registerBody = @{
    firstName = "Keyur"
    lastName = "Ajani"
    milkCompanyName = "Surat Fresh Milk Cooperative"
    phoneNumber = "9327304535"
    email = "keyur@freshmilk.com"
    password = "coopPassword123"
    role = "DELIVERY_MAN"
    telegramId = $null
    deviceId = "mobile_android_uuid_12345"
    upiId = "merchantvpa@ybl"
} | ConvertTo-Json
try {
    $registerResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/auth/register" -Method Post -Body $registerBody -Headers $headers
    $registerResponse | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 2. Login User
Write-Output "`n--- 2. Logging In ---"
$loginBody = @{
    phoneNumber = "9327304535"
    password = "coopPassword123"
    deviceId = "mobile_android_uuid_12345"
} | ConvertTo-Json
try {
    $loginResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/auth/login" -Method Post -Body $loginBody -Headers $headers
    $loginResponse | ConvertTo-Json -Depth 5
    $token = $loginResponse.token
} catch {
    # Handled inside function
}

if (-not $token) {
    Write-Host "Authentication failed, stopping tests." -ForegroundColor Red
    exit
}

# Set Auth Headers
$authHeaders = @{
    "Content-Type" = "application/json; charset=utf-8"
    "Authorization" = "Bearer $token"
}

# 3. Save Milk Category
Write-Output "`n--- 3. Saving Milk Category ---"
$categoryBody = @{
    categoryName = "Cow Milk"
    pricePerLiter = 60.0
    active = $true
} | ConvertTo-Json
try {
    $categoryResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/milk-categories/save" -Method Post -Body $categoryBody -Headers $authHeaders
    $categoryResponse | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 4. Save Customers
Write-Output "`n--- 4. Saving Customer 1 (Aarav Mehta) ---"
$customerBody1 = @{
    customerName = "Aarav Mehta"
    phoneNumber = "9988776655"
    address = "G-402 Shanti Niketan, Surat"
    latitude = 21.1702
    longitude = 72.8311
    milkQuantity = 1.5
    active = $true
    telegramId = "12345678"
    milkCategory = @{
        id = 1
    }
    user = @{
        id = 1
    }
} | ConvertTo-Json
try {
    $customerResponse1 = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/save" -Method Post -Body $customerBody1 -Headers $authHeaders
    $customerResponse1 | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

Write-Output "`n--- 4b. Saving Customer 2 (abs - 8401516824) ---"
$customerBody2 = @{
    customerName = "abs"
    phoneNumber = "8401516824"
    address = "B-201 Green Valley, Surat"
    latitude = 21.1824
    longitude = 72.8422
    milkQuantity = 2.0
    active = $true
    telegramId = $null
    milkCategory = @{
        id = 1
    }
    user = @{
        id = 1
    }
} | ConvertTo-Json
try {
    $customerResponse2 = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/save" -Method Post -Body $customerBody2 -Headers $authHeaders
    $customerResponse2 | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 5. Telegram Webhook Onboarding & Mapping Flow
Write-Output "`n--- 5a. Webhook: Unregistered user sends /start command ---"
try {
    $webhookResponse1 = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_unregistered_start.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponse1"
} catch {
    # Handled inside function
}

Write-Output "`n--- 5b. Webhook: Milkman shares contact (phone 9327304535) ---"
try {
    $webhookResponse2 = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_milkman_share_contact.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponse2"
} catch {
    # Handled inside function
}

Write-Output "`n--- 5c. Webhook: Customer abs shares contact (phone 8401516824) ---"
try {
    $webhookResponse3 = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_abs_share_contact.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponse3"
} catch {
    # Handled inside function
}

Write-Output "`n--- 5d. Fetching Updated Customer 2 Details from Server to verify telegramId ---"
try {
    $updatedCustomer2 = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/2" -Method Get -Headers $authHeaders
    $updatedCustomer2 | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

Write-Output "`n--- 5e. Webhook: Newly linked Customer abs sends /bill command ---"
try {
    $webhookResponse5 = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_registered_bill.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponse5"
} catch {
    # Handled inside function
}

Write-Output "`n--- 5f. Webhook: Customer abs sends custom /pause 10 command ---"
try {
    $webhookResponsePause = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_pause_custom.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponsePause"
} catch {
    # Handled inside function
}

Write-Output "`n--- 5g. Updating Milkman live location to trigger tracking test ---"
try {
    $trackingBody = @{
        userId = 1
        latitude = 21.1800
        longitude = 72.8400
        speed = 15.0
    } | ConvertTo-Json
    $trackingResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/tracking/update" -Method Post -Body $trackingBody -Headers $authHeaders
    $trackingResponse | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

Write-Output "`n--- 5h. Webhook: Customer abs sends /track command ---"
try {
    $webhookResponseTrack = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_track.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseTrack"
} catch {
    # Handled inside function
}

Write-Output "`n--- 5i. Webhook: Customer abs asks where the milkman is in Gujarati (NLP Location query) ---"
try {
    $webhookResponseWhere = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_where.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseWhere"
} catch {
    # Handled inside function
}

# 6. Telegram Webhook for Aarav Mehta (Llama 3/Gujarati request)
Write-Output "`n--- 6. Sending Telegram Webhook Update for Aarav Mehta ---"
try {
    $webhookResponse4 = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_payload.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponse4"
} catch {
    # Handled inside function
}

# 6b. Fetch Customer Details again to check extra milk values updated in DB
Write-Output "`n--- 6b. Fetching Updated Customer 1 Details ---"
try {
    $updatedCustomer1 = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/1" -Method Get -Headers $authHeaders
    $updatedCustomer1 | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 6c. Fetch Customer 2 (abs) Details to verify custom pause values updated in DB
Write-Output "`n--- 6c. Fetching Updated Customer 2 Details to verify pause days ---"
try {
    $updatedCustomer2Pause = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/2" -Method Get -Headers $authHeaders
    $updatedCustomer2Pause | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

Write-Output "`n--- 6d. Webhook: Customer abs sends direct /extra 3 command ---"
try {
    $webhookResponseExtra = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_extra_custom.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseExtra"
} catch {
    # Handled inside function
}

Write-Output "`n--- 6e. Webhook: Customer abs sends a 2-second voice message (Mock Live Tracking) ---"
try {
    $webhookResponseVoice = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_voice_tracking.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseVoice"
} catch {
    # Handled inside function
}

Write-Output "`n--- 6f. Fetching Updated Customer 2 Details to verify extra milk quantity and days ---"
try {
    $updatedCustomer2Extra = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/2" -Method Get -Headers $authHeaders
    $updatedCustomer2Extra | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}


# 6g. Webhook: Customer abs sends extra milk cancel command (/extra 0) ---
Write-Output "`n--- 6g. Webhook: Customer abs sends extra milk cancel command (/extra 0) ---"
try {
    $webhookResponseCancelCmd = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_extra_cancel_command.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseCancelCmd"
} catch {
    # Handled inside function
}

# 6h. Fetching Updated Customer 2 Details to verify extra milk is cancelled (0.0) ---
Write-Output "`n--- 6h. Fetching Updated Customer 2 Details to verify extra milk is cancelled (0.0) ---"
try {
    $updatedCustomer2Cancelled = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/2" -Method Get -Headers $authHeaders
    $updatedCustomer2Cancelled | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 6i. Setting extra milk back to test conversational cancel ---
Write-Output "`n--- 6i. Re-requesting extra milk to test conversational cancel ---"
try {
    $webhookResponseExtra2 = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_extra_custom.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseExtra2"
} catch {
    # Handled inside function
}

# 6j. Webhook: Customer abs cancels extra milk conversationally in Gujarati ---
Write-Output "`n--- 6j. Webhook: Customer abs cancels extra milk conversationally in Gujarati ---"
try {
    $webhookResponseCancelConv = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_extra_cancel_conv.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseCancelConv"
} catch {
    # Handled inside function
}

# 6k. Fetching Updated Customer 2 Details again to verify extra milk is cancelled (0.0) ---
Write-Output "`n--- 6k. Fetching Updated Customer 2 Details to verify conversational cancellation ---"
try {
    $updatedCustomer2CancelledConv = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/2" -Method Get -Headers $authHeaders
    $updatedCustomer2CancelledConv | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 6l. Setting extra milk back so that subsequent delivery tests run correctly ---
Write-Output "`n--- 6l. Re-setting extra milk back for subsequent delivery tests ---"
try {
    $webhookResponseExtraFinal = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_extra_custom.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseExtraFinal"
} catch {
    # Handled inside function
}

# 6m. Webhook: Customer abs sends an 8-second voice message (Pause Delivery) ---
Write-Output "`n--- 6m. Webhook: Customer abs sends an 8-second voice message (Mock Pause Delivery) ---"
try {
    $webhookResponseVoicePause = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_voice_pause.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseVoicePause"
} catch {
    # Handled inside function
}

# Verify Customer 2 is Paused
Write-Output "`n--- 6n. Fetching Updated Customer 2 Details to verify Paused state ---"
try {
    $updatedCustomer2VoicePause = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/2" -Method Get -Headers $authHeaders
    $updatedCustomer2VoicePause | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 6o. Webhook: Customer abs sends a 17-second voice message (Resume Delivery) ---
Write-Output "`n--- 6o. Webhook: Customer abs sends a 17-second voice message (Mock Resume Delivery) ---"
try {
    $webhookResponseVoiceResume = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_voice_resume.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseVoiceResume"
} catch {
    # Handled inside function
}

# Verify Customer 2 is Resumed
Write-Output "`n--- 6p. Fetching Updated Customer 2 Details to verify Resumed state ---"
try {
    $updatedCustomer2VoiceResume = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/2" -Method Get -Headers $authHeaders
    $updatedCustomer2VoiceResume | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 6q. Webhook: Customer abs sends a 5-second voice message (Extra Milk) ---
Write-Output "`n--- 6q. Webhook: Customer abs sends a 5-second voice message (Mock Extra Milk) ---"
try {
    $webhookResponseVoiceExtra = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_voice_extra.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseVoiceExtra"
} catch {
    # Handled inside function
}

# Verify Customer 2 Extra Milk set to 2.0
Write-Output "`n--- 6r. Fetching Updated Customer 2 Details to verify Extra Milk quantity ---"
try {
    $updatedCustomer2VoiceExtra = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/2" -Method Get -Headers $authHeaders
    $updatedCustomer2VoiceExtra | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 6s. Webhook: Customer abs sends a 14-second voice message (Cancel Extra Milk) ---
Write-Output "`n--- 6s. Webhook: Customer abs sends a 14-second voice message (Mock Cancel Extra Milk) ---"
try {
    $webhookResponseVoiceCancel = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_voice_cancel.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseVoiceCancel"
} catch {
    # Handled inside function
}

# Verify Customer 2 Extra Milk set to 0.0
Write-Output "`n--- 6t. Fetching Updated Customer 2 Details to verify Extra Milk is cancelled ---"
try {
    $updatedCustomer2VoiceCancel = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/2" -Method Get -Headers $authHeaders
    $updatedCustomer2VoiceCancel | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 6u. Webhook: Customer abs sends a 11-second voice message (Check Bill) ---
Write-Output "`n--- 6u. Webhook: Customer abs sends a 11-second voice message (Mock Check Bill) ---"
try {
    $webhookResponseVoiceBill = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_voice_bill.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseVoiceBill"
} catch {
    # Handled inside function
}

# 6v. Re-setting extra milk back for subsequent delivery tests ---
Write-Output "`n--- 6v. Re-setting extra milk back for subsequent delivery tests ---"
try {
    $webhookResponseExtraFinalVoice = curl.exe -s -X POST -H "Content-Type: application/json; charset=utf-8" --data-binary '@webhook_customer_extra_custom.json' http://localhost:8081/api/telegram/webhook
    Write-Output "Response: $webhookResponseExtraFinalVoice"
} catch {
    # Handled inside function
}

# 7. Start Delivery Session
Write-Output "`n--- 7. Starting Delivery Session ---"
$sessionBody = @{
    milkmanId = 1
    loadedMilk = 120.0
} | ConvertTo-Json
try {
    $sessionResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/deliveries/start-session" -Method Post -Body $sessionBody -Headers $authHeaders
    $sessionResponse | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 8. Mark Customer Delivered
Write-Output "`n--- 8. Marking Customer 1 Delivered ---"
try {
    $deliveredResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/delivered/1" -Method Post -Headers $authHeaders
    Write-Output "Response: $deliveredResponse"
} catch {
    # Handled inside function
}

Write-Output "`n--- 8b. Marking Customer 2 Delivered (checking extra milk reset) ---"
try {
    $deliveredResponse2 = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/delivered/2" -Method Post -Headers $authHeaders
    Write-Output "Response: $deliveredResponse2"
} catch {
    # Handled inside function
}

Write-Output "`n--- 8c. Fetching Customer 2 Details again to verify extra milk reset to 0.0 and days to 0 ---"
try {
    $updatedCustomer2Reset = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/2" -Method Get -Headers $authHeaders
    $updatedCustomer2Reset | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 9. Get Session Summary
Write-Output "`n--- 9. Fetching Session Summary ---"
try {
    $summaryResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/deliveries/session-summary/1" -Method Get -Headers $authHeaders
    $summaryResponse | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 10. Generate Bill
Write-Output "`n--- 10. Generating Monthly Bill ---"
$billBody = @{
    customerId = 1
    month = 6
    year = 2026
} | ConvertTo-Json
try {
    $billResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/payments/generate-bill" -Method Post -Body $billBody -Headers $authHeaders
    $billResponse | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 11. Fetch QR Code Base64
Write-Output "`n--- 11. Fetching UPI Payment QR Code ---"
try {
    $qrResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/payments/qr/1" -Method Get -Headers $authHeaders
    $qrResponse | ConvertTo-Json -Depth 5
} catch {
    # Handled inside function
}

# 12. Fetch Invoice PDF Bytes length
Write-Output "`n--- 12. Fetching Invoice PDF Bytes ---"
try {
    $pdfResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/payments/invoice/1" -Method Get -Headers $authHeaders -UseBasicParsing
    Write-Output "PDF byte array length received: $($pdfResponse.Content.Length)"
} catch {
    # Handled inside function
}

# 13. Export Monthly Report
Write-Output "`n--- 13. Exporting Monthly Excel Report ---"
try {
    $reportResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/reports/monthly?month=6&year=2026&format=excel" -Method Get -Headers $authHeaders -UseBasicParsing
    Write-Output "Excel byte array length received: $($reportResponse.Content.Length)"
} catch {
    # Handled inside function
}
# 14. Test Web Deactivation (Delete Plan)
Write-Output "`n--- 14a. Admin Deleting/Cancelling Customer 2 Plan ---"
try {
    $deleteResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/delete/2" -Method Delete -Headers $authHeaders
    Write-Output "Response: $deleteResponse"
} catch { }

# Verify Customer 2 state is inactive and states reset
Write-Output "`n--- 14b. Verifying Customer 2 Inactive & Reset State ---"
try {
    $inactiveCustomer2 = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/2" -Method Get -Headers $authHeaders
    $inactiveCustomer2 | ConvertTo-Json -Depth 5
} catch { }

# 15. Test Web Reactivation (Activate Plan)
Write-Output "`n--- 15a. Admin Reactivating/Starting Customer 2 Plan ---"
try {
    $activateResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/activate/2" -Method Put -Headers $authHeaders
    Write-Output "Response: $activateResponse"
} catch { }

# Verify Customer 2 state is active
Write-Output "`n--- 15b. Verifying Customer 2 Active State ---"
try {
    $activeCustomer2 = Invoke-RestWithDetails -Uri "http://localhost:8081/api/customers/2" -Method Get -Headers $authHeaders
    $activeCustomer2 | ConvertTo-Json -Depth 5
} catch { }

# 16. Razorpay Subscription Payments Test
Write-Output "`n--- 16a. Creating Subscription Plan ---"
$planBody = @{
    name = "Gold Plan"
    price = 599.0
    durationDays = 30
    maxCustomers = 50
    featuresJson = "['E2E', 'Tracking', 'PDF Invoices']"
} | ConvertTo-Json
try {
    $planResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/super-admin/plans" -Method Post -Body $planBody -Headers $authHeaders
    $planResponse | ConvertTo-Json -Depth 5
    $planId = $planResponse.id
} catch { }

Write-Output "`n--- 16b. Initiating Razorpay Subscription Checkout ---"
try {
    $checkoutResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/subscriptions/checkout?userId=1&planId=$planId" -Method Post -Headers $authHeaders
    $checkoutResponse | ConvertTo-Json -Depth 5
    $orderId = $checkoutResponse.razorpayOrderId
} catch { }

Write-Output "`n--- 16c. Verifying Razorpay Subscription Payment ---"
$verifyBody = @{
    razorpayOrderId = $orderId
    razorpayPaymentId = "pay_mock_e2e_12345"
    razorpaySignature = "mock_signature"
    userId = 1
    planId = $planId
} | ConvertTo-Json
try {
    $verifyResponse = Invoke-RestWithDetails -Uri "http://localhost:8081/api/subscriptions/verify" -Method Post -Body $verifyBody -Headers $authHeaders
    $verifyResponse | ConvertTo-Json -Depth 5
} catch { }

Write-Output "`n=========================================================="
Write-Output "TESTING COMPLETED!"
Write-Output "=========================================================="
