$ErrorActionPreference = 'Stop'

Write-Host 'Cabinet AI launcher' -ForegroundColor Cyan
$key = Read-Host 'Enter OpenAI API Key (input is hidden)' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($key)
try {
    $env:AI_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}
$env:AI_MODEL = 'gpt-5.2'
$env:AI_API_URL = 'https://console.mirrorcoding.xyz/v1/chat/completions'
Write-Host 'Starting http://localhost:8081 ...' -ForegroundColor Green
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
