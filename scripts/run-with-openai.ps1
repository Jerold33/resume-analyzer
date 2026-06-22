param(
    [string]$Profile = "dev"
)

# Prompt for the OpenAI API key securely if not set
if (-not $env:OPENAI_API_KEY -or $env:OPENAI_API_KEY -eq "") {
    $secure = Read-Host -AsSecureString "Enter OpenAI API key (will not echo)"
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
    $env:OPENAI_API_KEY = $plain
}

$env:SPRING_PROFILES_ACTIVE = $Profile

Write-Host "Starting resume-analyzer (profile=$Profile) with OpenAI key set in environment (hidden)."

java -jar ..\target\resume-analyzer-0.0.1-SNAPSHOT.jar
