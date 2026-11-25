#requires -Version 7.2
[CmdletBinding()]
param(
  [string]$GatewayUrl = '',
  [string]$EnvFile = ''
)

$ErrorActionPreference = 'Stop'

function Import-DemoEnvFile {
  <# Carga pares clave=valor desde un archivo .env sin sobrescribir variables ya definidas. #>
  param([Parameter(Mandatory)][string]$Path)

  if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    throw "No se encontró el archivo de entorno: $Path"
  }

  $loaded = @{}
  foreach ($rawLine in [IO.File]::ReadAllLines($Path, [Text.UTF8Encoding]::new($false))) {
    $line = $rawLine.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
      continue
    }
    $eqIndex = $line.IndexOf('=')
    if ($eqIndex -lt 1) {
      continue
    }
    $key = $line.Substring(0, $eqIndex).Trim()
    $value = $line.Substring($eqIndex + 1).Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
      if ($value.Length -ge 2) {
        $value = $value.Substring(1, $value.Length - 2)
      }
    }
    $loaded[$key] = $value
  }
  return $loaded
}

function Resolve-DemoConfiguration {
  <# Resuelve y valida la configuración requerida desde parámetros, archivo .env o variables de entorno. #>
  param(
    [string]$GatewayUrlParam = '',
    [string]$EnvFileParam = '',
    [string]$RepoRoot = ''
  )

  if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
  }

  $fileValues = @{}
  if (-not [string]::IsNullOrWhiteSpace($EnvFileParam)) {
    $resolvedEnv = if ([IO.Path]::IsPathRooted($EnvFileParam)) { $EnvFileParam } else { Join-Path (Get-Location).Path $EnvFileParam }
    $fileValues = Import-DemoEnvFile -Path $resolvedEnv
  } else {
    $defaultEnv = Join-Path $RepoRoot '.env.demo-company'
    if (Test-Path -LiteralPath $defaultEnv -PathType Leaf) {
      $fileValues = Import-DemoEnvFile -Path $defaultEnv
    }
  }

  $getSetting = {
    param([string]$Name, [string]$Default = '')
    $envVal = [Environment]::GetEnvironmentVariable($Name)
    if (-not [string]::IsNullOrWhiteSpace($envVal)) {
      return $envVal.Trim()
    }
    if ($fileValues.ContainsKey($Name) -and -not [string]::IsNullOrWhiteSpace([string]$fileValues[$Name])) {
      return ([string]$fileValues[$Name]).Trim()
    }
    return $Default
  }

  $requiredVars = @(
    'DEMO_COMPANY_RUC',
    'DEMO_ADMIN_EMAIL',
    'DEMO_ADMIN_PASSWORD',
    'DEMO_OPS_EMAIL',
    'DEMO_OPS_PASSWORD',
    'DEMO_DRIVER1_EMAIL',
    'DEMO_DRIVER1_PASSWORD',
    'DEMO_DRIVER2_EMAIL',
    'DEMO_DRIVER2_PASSWORD'
  )

  $missing = [System.Collections.Generic.List[string]]::new()
  $placeholders = [System.Collections.Generic.List[string]]::new()
  $values = @{}

  foreach ($varName in $requiredVars) {
    $val = & $getSetting $varName ''
    if ([string]::IsNullOrWhiteSpace($val)) {
      $missing.Add($varName)
    } elseif ($val -like 'REPLACE_WITH_*') {
      $placeholders.Add($varName)
    }
    $values[$varName] = $val
  }

  if ($missing.Count -gt 0) {
    throw ("Faltan variables de entorno requeridas: " + ($missing -join ', ') + ". Defínalas en el entorno o en .env.demo-company.")
  }
  if ($placeholders.Count -gt 0) {
    throw ("Las siguientes variables conservan valores de plantilla: " + ($placeholders -join ', ') + ". Reemplácelas antes de ejecutar el script.")
  }

  if ($values['DEMO_COMPANY_RUC'] -notmatch '^\d{11}$') {
    throw "DEMO_COMPANY_RUC debe tener exactamente 11 dígitos numéricos."
  }

  foreach ($pwdVar in @('DEMO_ADMIN_PASSWORD', 'DEMO_OPS_PASSWORD', 'DEMO_DRIVER1_PASSWORD', 'DEMO_DRIVER2_PASSWORD')) {
    $len = $values[$pwdVar].Length
    if ($len -lt 8 -or $len -gt 72) {
      throw "$pwdVar debe tener entre 8 y 72 caracteres."
    }
  }

  $resolvedGateway = if (-not [string]::IsNullOrWhiteSpace($GatewayUrlParam)) {
    $GatewayUrlParam.Trim()
  } else {
    & $getSetting 'RINDE_GATEWAY_URL' 'http://localhost:8000'
  }
  $resolvedGateway = $resolvedGateway.TrimEnd('/')

  return [pscustomobject]@{
    GatewayUrl             = $resolvedGateway
    CompanyTradeName       = & $getSetting 'DEMO_COMPANY_TRADE_NAME' 'Transportes Ruta Segura SAC'
    CompanyRuc             = $values['DEMO_COMPANY_RUC']
    CompanyPhone           = & $getSetting 'DEMO_COMPANY_PHONE' '+51 1 445 8899'
    CompanyAddress         = & $getSetting 'DEMO_COMPANY_ADDRESS' 'Av. Nicolas Arriola 1200, La Victoria, Lima'
    AdminName              = & $getSetting 'DEMO_ADMIN_NAME' 'Ana Rojas Paredes'
    AdminEmail             = $values['DEMO_ADMIN_EMAIL'].ToLowerInvariant()
    AdminPassword          = $values['DEMO_ADMIN_PASSWORD']
    OpsName                = & $getSetting 'DEMO_OPS_NAME' 'Carlos Mendoza Quispe'
    OpsEmail               = $values['DEMO_OPS_EMAIL'].ToLowerInvariant()
    OpsPassword            = $values['DEMO_OPS_PASSWORD']
    Driver1Name            = & $getSetting 'DEMO_DRIVER1_NAME' 'Luis Quispe Mamani'
    Driver1Email           = $values['DEMO_DRIVER1_EMAIL'].ToLowerInvariant()
    Driver1Password        = $values['DEMO_DRIVER1_PASSWORD']
    Driver2Name            = & $getSetting 'DEMO_DRIVER2_NAME' 'Marco Paredes Huaman'
    Driver2Email           = $values['DEMO_DRIVER2_EMAIL'].ToLowerInvariant()
    Driver2Password        = $values['DEMO_DRIVER2_PASSWORD']
    PlatformOwnerEmail     = (& $getSetting 'PLATFORM_OWNER_EMAIL' '').ToLowerInvariant()
    PlatformOwnerPassword  = & $getSetting 'PLATFORM_OWNER_PASSWORD' ''
    DemoVerificationToken  = & $getSetting 'DEMO_VERIFICATION_TOKEN' ''
    DemoTenantId           = & $getSetting 'DEMO_TENANT_ID' ''
  }
}

function New-DemoPngBytes {
  <# Devuelve un PNG válido de 1x1 píxel generado en memoria para comprobantes de prueba. #>
  return [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=')
}

function Get-TokenFromMailboxActionPath {
  <# Extrae y decodifica tenant y token desde actionPath del buzón de demostración. #>
  param([Parameter(Mandatory)][string]$ActionPath)

  $queryIndex = $ActionPath.IndexOf('?')
  if ($queryIndex -lt 0) {
    return [pscustomobject]@{ TenantId = $null; Token = $null }
  }
  $query = $ActionPath.Substring($queryIndex + 1)
  $tenantId = $null
  $token = $null
  foreach ($pair in $query.Split('&')) {
    $parts = $pair.Split('=', 2)
    if ($parts.Length -ne 2) { continue }
    $k = [Uri]::UnescapeDataString($parts[0])
    $v = [Uri]::UnescapeDataString($parts[1])
    if ($k -eq 'tenant') { $tenantId = $v }
    if ($k -eq 'token') { $token = $v }
  }
  return [pscustomobject]@{ TenantId = $tenantId; Token = $token }
}

function Read-JsonBody {
  <# Convierte el contenido de una respuesta HTTP en objeto PowerShell cuando existe cuerpo JSON. #>
  param($Response)

  if ($null -eq $Response -or $null -eq $Response.Content) {
    return $null
  }
  $text = if ($Response.Content -is [byte[]]) {
    [Text.Encoding]::UTF8.GetString($Response.Content)
  } else {
    [string]$Response.Content
  }
  if ([string]::IsNullOrWhiteSpace($text)) {
    return $null
  }
  return $text | ConvertFrom-Json -Depth 20
}

function Format-ProblemDetailMessage {
  <# Extrae un mensaje legible desde respuestas RFC 7807 ProblemDetail sin exponer secretos. #>
  param(
    [string]$Method,
    [string]$Path,
    [int]$StatusCode,
    $ParsedBody
  )

  $detail = ''
  if ($null -ne $ParsedBody) {
    if ($ParsedBody.PSObject.Properties['detail'] -and -not [string]::IsNullOrWhiteSpace([string]$ParsedBody.detail)) {
      $detail = [string]$ParsedBody.detail
    } elseif ($ParsedBody.PSObject.Properties['title'] -and -not [string]::IsNullOrWhiteSpace([string]$ParsedBody.title)) {
      $detail = [string]$ParsedBody.title
    }
  }
  if ([string]::IsNullOrWhiteSpace($detail)) {
    return "HTTP $StatusCode en $Method $Path"
  }
  return "HTTP $StatusCode en $Method ${Path}: $detail"
}

function Invoke-RindeApi {
  <# Ejecuta una petición JSON HTTP contra el gateway y valida códigos esperados. #>
  param(
    [Parameter(Mandatory)][string]$GatewayUrl,
    [Parameter(Mandatory)][string]$Method,
    [Parameter(Mandatory)][string]$Path,
    $Body = $null,
    [string]$Token = '',
    [int[]]$ExpectedStatus = @(200)
  )

  $uri = $GatewayUrl.TrimEnd('/') + $Path
  $headers = @{ Accept = 'application/json' }
  if (-not [string]::IsNullOrWhiteSpace($Token)) {
    $headers['Authorization'] = "Bearer $Token"
  }

  $params = @{
    Method              = $Method
    Uri                 = $uri
    Headers             = $headers
    SkipHttpErrorCheck  = $true
    TimeoutSec          = 25
  }
  if ($null -ne $Body) {
    $params['Body'] = ($Body | ConvertTo-Json -Depth 10 -Compress)
    $params['ContentType'] = 'application/json; charset=utf-8'
  }

  $response = Invoke-WebRequest @params
  $statusCode = [int]$response.StatusCode
  $parsed = $null
  try {
    $parsed = Read-JsonBody -Response $response
  } catch {
    $parsed = $null
  }

  if ($ExpectedStatus -notcontains $statusCode) {
    throw (Format-ProblemDetailMessage -Method $Method -Path $Path -StatusCode $statusCode -ParsedBody $parsed)
  }

  return [pscustomobject]@{
    StatusCode = $statusCode
    Data       = $parsed
  }
}

function Invoke-RindeMultipartPng {
  <# Sube un comprobante PNG en memoria usando multipart/form-data en el campo file. #>
  param(
    [Parameter(Mandatory)][string]$GatewayUrl,
    [Parameter(Mandatory)][string]$Path,
    [Parameter(Mandatory)][string]$Token,
    [Parameter(Mandatory)][byte[]]$PngBytes,
    [string]$FileName = 'comprobante-demo.png'
  )

  $uri = $GatewayUrl.TrimEnd('/') + $Path
  $client = [System.Net.Http.HttpClient]::new()
  $client.Timeout = [TimeSpan]::FromSeconds(25)
  try {
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, $uri)
    $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $Token)

    $multipart = [System.Net.Http.MultipartFormDataContent]::new()
    $fileContent = [System.Net.Http.ByteArrayContent]::new($PngBytes)
    $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('image/png')
    $multipart.Add($fileContent, 'file', $FileName)
    $request.Content = $multipart

    $response = $client.SendAsync($request).GetAwaiter().GetResult()
    $statusCode = [int]$response.StatusCode
    $rawBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $parsed = if ([string]::IsNullOrWhiteSpace($rawBody)) { $null } else {
      try { $rawBody | ConvertFrom-Json -Depth 20 } catch { $null }
    }
    if ($statusCode -notin @(200, 201)) {
      throw (Format-ProblemDetailMessage -Method 'POST' -Path $Path -StatusCode $statusCode -ParsedBody $parsed)
    }
    return [pscustomobject]@{
      StatusCode = $statusCode
      Data       = $parsed
    }
  } finally {
    $client.Dispose()
  }
}

function Get-DemoMailboxToken {
  <# Busca y abre el enlace vigente más reciente en el buzón de demostración para un correo y propósito. #>
  param(
    [Parameter(Mandatory)][string]$GatewayUrl,
    [Parameter(Mandatory)][string]$PlatformToken,
    [Parameter(Mandatory)][string]$RecipientEmail,
    [Parameter(Mandatory)][string]$Purpose
  )

  $encodedSearch = [Uri]::EscapeDataString($RecipientEmail)
  $listRes = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' `
    -Path "/api/v1/platform/demo-mailbox/messages?search=$encodedSearch&purpose=$Purpose&page=0&size=20" `
    -Token $PlatformToken -ExpectedStatus @(200, 401, 403, 404)

  if ($listRes.StatusCode -ne 200 -or $null -eq $listRes.Data -or $null -eq $listRes.Data.items) {
    return $null
  }

  $match = $listRes.Data.items |
    Where-Object {
      $_.recipient -and $_.recipient.ToLowerInvariant() -eq $RecipientEmail.ToLowerInvariant() -and
      $_.purpose -eq $Purpose -and
      $_.state -eq 'ACTIVE'
    } |
    Sort-Object createdAt -Descending |
    Select-Object -First 1

  if ($null -eq $match) {
    return $null
  }

  $detailRes = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' `
    -Path "/api/v1/platform/demo-mailbox/messages/$($match.id)" `
    -Token $PlatformToken -ExpectedStatus @(200, 404)

  if ($detailRes.StatusCode -ne 200 -or $null -eq $detailRes.Data -or [string]::IsNullOrWhiteSpace([string]$detailRes.Data.actionPath)) {
    return $null
  }

  return Get-TokenFromMailboxActionPath -ActionPath ([string]$detailRes.Data.actionPath)
}

function Get-PlatformOwnerToken {
  <# Inicia sesión como dueño de plataforma si existen credenciales configuradas y el entorno lo permite. #>
  param(
    [Parameter(Mandatory)][string]$GatewayUrl,
    [string]$Email,
    [string]$Password
  )

  if ([string]::IsNullOrWhiteSpace($Email) -or [string]::IsNullOrWhiteSpace($Password)) {
    return ''
  }

  $res = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' `
    -Path '/api/v1/platform/auth/sign-in' `
    -Body @{ email = $Email; password = $Password } `
    -ExpectedStatus @(200, 401, 403, 404)

  if ($res.StatusCode -eq 200 -and $null -ne $res.Data -and -not [string]::IsNullOrWhiteSpace([string]$res.Data.accessToken)) {
    return [string]$res.Data.accessToken
  }
  return ''
}

function Wait-UntilCondition {
  <# Espera a que una condición asíncrona (eventos RabbitMQ) se cumpla dentro del tiempo límite. #>
  param(
    [Parameter(Mandatory)][scriptblock]$Probe,
    [Parameter(Mandatory)][string]$TimeoutMessage,
    [int]$TimeoutSeconds = 20,
    [int]$IntervalMilliseconds = 400
  )

  $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
  do {
    $result = & $Probe
    if ($null -ne $result) {
      return $result
    }
    Start-Sleep -Milliseconds $IntervalMilliseconds
  } while ([DateTime]::UtcNow -lt $deadline)

  throw $TimeoutMessage
}

function Ensure-DemoTenantAndUsers {
  <# Garantiza empresa verificada y las cuatro cuentas activas (ADMIN, OPS, 2 DRIVER) de forma idempotente. #>
  param([Parameter(Mandatory)]$Config)

  $gw = $Config.GatewayUrl
  $platformToken = ''
  $getPlatformToken = {
    if ([string]::IsNullOrWhiteSpace($script:cachedPlatformToken)) {
      $script:cachedPlatformToken = Get-PlatformOwnerToken -GatewayUrl $gw -Email $Config.PlatformOwnerEmail -Password $Config.PlatformOwnerPassword
    }
    return $script:cachedPlatformToken
  }
  $script:cachedPlatformToken = ''

  $tenantCreated = $false
  $tenantVerified = $false
  $tenantId = $null

  $adminSignIn = Invoke-RindeApi -GatewayUrl $gw -Method 'POST' -Path '/api/v1/auth/sign-in' `
    -Body @{ email = $Config.AdminEmail; password = $Config.AdminPassword } `
    -ExpectedStatus @(200, 401, 403)

  if ($adminSignIn.StatusCode -eq 401) {
    $registerRes = Invoke-RindeApi -GatewayUrl $gw -Method 'POST' -Path '/api/v1/tenants' `
      -Body @{
        tradeName             = $Config.CompanyTradeName
        ruc                   = $Config.CompanyRuc
        administratorFullName = $Config.AdminName
        administratorEmail    = $Config.AdminEmail
        password              = $Config.AdminPassword
      } `
      -ExpectedStatus @(201, 409)

    if ($registerRes.StatusCode -eq 409) {
      throw "La empresa o el correo administrador ($($Config.AdminEmail)) ya existen con credenciales distintas. Verifique .env.demo-company."
    }

    $tenantCreated = $true
    $tenantId = [string]$registerRes.Data.id
  } elseif ($adminSignIn.StatusCode -eq 403) {
    $detail = if ($adminSignIn.Data -and $adminSignIn.Data.detail) { [string]$adminSignIn.Data.detail } else { 'Acceso denegado.' }
    if ($detail -notlike '*confirm*' -and $detail -notlike '*verific*') {
      throw "El acceso del administrador fue rechazado (403): $detail"
    }
  }

  if ($adminSignIn.StatusCode -ne 200) {
    $verificationToken = $Config.DemoVerificationToken
    if ([string]::IsNullOrWhiteSpace($tenantId) -and -not [string]::IsNullOrWhiteSpace($Config.DemoTenantId)) {
      $tenantId = $Config.DemoTenantId
    }
    if ([string]::IsNullOrWhiteSpace($verificationToken) -or [string]::IsNullOrWhiteSpace($tenantId)) {
      $pt = & $getPlatformToken
      if (-not [string]::IsNullOrWhiteSpace($pt)) {
        $mb = Get-DemoMailboxToken -GatewayUrl $gw -PlatformToken $pt -RecipientEmail $Config.AdminEmail -Purpose 'EMAIL_VERIFICATION'
        if ($null -ne $mb) {
          if ([string]::IsNullOrWhiteSpace($tenantId)) { $tenantId = $mb.TenantId }
          if ([string]::IsNullOrWhiteSpace($verificationToken)) { $verificationToken = $mb.Token }
        }
      }
    }

    if ([string]::IsNullOrWhiteSpace($tenantId) -or [string]::IsNullOrWhiteSpace($verificationToken)) {
      throw "La empresa $($Config.CompanyRuc) requiere verificación y el buzón de demostración no devolvió el enlace. Verifique la empresa por correo o configure PLATFORM_OWNER_EMAIL/PLATFORM_OWNER_PASSWORD o DEMO_VERIFICATION_TOKEN."
    }

    $null = Invoke-RindeApi -GatewayUrl $gw -Method 'POST' -Path "/api/v1/tenants/$tenantId/verification" `
      -Body @{ token = $verificationToken } -ExpectedStatus @(200)
    $tenantVerified = $true

    $adminSignIn = Invoke-RindeApi -GatewayUrl $gw -Method 'POST' -Path '/api/v1/auth/sign-in' `
      -Body @{ email = $Config.AdminEmail; password = $Config.AdminPassword } `
      -ExpectedStatus @(200)
  }

  $adminToken = [string]$adminSignIn.Data.accessToken

  $currentTenantRes = Invoke-RindeApi -GatewayUrl $gw -Method 'GET' -Path '/api/v1/tenants/current' `
    -Token $adminToken -ExpectedStatus @(200, 404, 501)
  if ($currentTenantRes.StatusCode -eq 200 -and $null -ne $currentTenantRes.Data) {
    $tenantId = [string]$currentTenantRes.Data.id
    if ([string]::IsNullOrWhiteSpace([string]$currentTenantRes.Data.phone) -or [string]::IsNullOrWhiteSpace([string]$currentTenantRes.Data.address)) {
      $null = Invoke-RindeApi -GatewayUrl $gw -Method 'PATCH' -Path '/api/v1/tenants/current' `
        -Token $adminToken `
        -Body @{
          tradeName = $Config.CompanyTradeName
          phone     = $Config.CompanyPhone
          address   = $Config.CompanyAddress
        } `
        -ExpectedStatus @(200, 404, 501)
    }
  } elseif ([string]::IsNullOrWhiteSpace($tenantId)) {
    $meRes = Invoke-RindeApi -GatewayUrl $gw -Method 'GET' -Path '/api/v1/users/me' `
      -Token $adminToken -ExpectedStatus @(200)
    if ($null -ne $meRes.Data -and $null -ne $meRes.Data.tenant -and $meRes.Data.tenant.id) {
      $tenantId = [string]$meRes.Data.tenant.id
    }
  }

  $usersRes = Invoke-RindeApi -GatewayUrl $gw -Method 'GET' -Path '/api/v1/users' -Token $adminToken -ExpectedStatus @(200)
  $existingUsers = @($usersRes.Data)

  $userSpecs = @(
    @{ Key = 'Ops'; Name = $Config.OpsName; Email = $Config.OpsEmail; Password = $Config.OpsPassword; Role = 'OPERATIONS_MANAGER' },
    @{ Key = 'Driver1'; Name = $Config.Driver1Name; Email = $Config.Driver1Email; Password = $Config.Driver1Password; Role = 'DRIVER' },
    @{ Key = 'Driver2'; Name = $Config.Driver2Name; Email = $Config.Driver2Email; Password = $Config.Driver2Password; Role = 'DRIVER' }
  )

  $usersCreated = 0
  $usersActivated = 0
  $sessions = @{ AdminToken = $adminToken }

  foreach ($spec in $userSpecs) {
    $user = $existingUsers | Where-Object { $_.email -and $_.email.ToLowerInvariant() -eq $spec.Email } | Select-Object -First 1
    if ($null -eq $user) {
      $createRes = Invoke-RindeApi -GatewayUrl $gw -Method 'POST' -Path '/api/v1/users' -Token $adminToken `
        -Body @{ fullName = $spec.Name; email = $spec.Email; role = $spec.Role } `
        -ExpectedStatus @(201)
      $user = $createRes.Data
      $usersCreated++
    }

    if ($user.status -eq 'INVITED') {
      $pt = & $getPlatformToken
      $invToken = $null
      if (-not [string]::IsNullOrWhiteSpace($pt)) {
        $mb = Get-DemoMailboxToken -GatewayUrl $gw -PlatformToken $pt -RecipientEmail $spec.Email -Purpose 'INVITATION'
        if ($null -ne $mb) { $invToken = $mb.Token }
      }
      if ([string]::IsNullOrWhiteSpace($invToken)) {
        throw "El usuario $($spec.Email) ($($spec.Role)) está en estado INVITED y el buzón de demostración no está disponible. Acepte su invitación desde el correo enviado y vuelva a ejecutar el script."
      }
      $null = Invoke-RindeApi -GatewayUrl $gw -Method 'POST' -Path '/api/v1/auth/password' `
        -Body @{ token = $invToken; password = $spec.Password } -ExpectedStatus @(204)
      $usersActivated++
    } elseif ($user.status -eq 'DISABLED') {
      throw "El usuario $($spec.Email) se encuentra DISABLED. Habilítelo o use otro correo en .env.demo-company."
    }

    $signIn = Invoke-RindeApi -GatewayUrl $gw -Method 'POST' -Path '/api/v1/auth/sign-in' `
      -Body @{ email = $spec.Email; password = $spec.Password } -ExpectedStatus @(200)

    $sessions[$spec.Key + 'User'] = $user
    $sessions[$spec.Key + 'Token'] = [string]$signIn.Data.accessToken
  }

  return [pscustomobject]@{
    TenantId       = $tenantId
    TenantCreated  = $tenantCreated
    TenantVerified = $tenantVerified
    UsersCreated   = $usersCreated
    UsersActivated = $usersActivated
    AdminToken     = $sessions.AdminToken
    OpsToken       = $sessions.OpsToken
    Driver1User    = $sessions.Driver1User
    Driver1Token   = $sessions.Driver1Token
    Driver2User    = $sessions.Driver2User
    Driver2Token   = $sessions.Driver2Token
  }
}

function Ensure-DemoFleet {
  <# Garantiza los 3 vehículos con sus estados de mantenimiento y los 2 conductores vinculados en Fleet. #>
  param(
    [Parameter(Mandatory)][string]$GatewayUrl,
    [Parameter(Mandatory)]$Auth,
    [Parameter(Mandatory)]$Config,
    [Parameter(Mandatory)][datetime]$Today
  )

  $opsToken = $Auth.OpsToken
  $vehiclesRes = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path '/api/v1/vehicles' -Token $opsToken -ExpectedStatus @(200)
  $existingVehicles = @($vehiclesRes.Data)

  $vehicleSpecs = @(
    @{
      Key = 'Ok'
      Plate = 'RIN-101'
      Brand = 'Volvo'
      Model = 'FH 460'
      Year = 2023
      CapacityKg = 25000.00
      MaintType = 'PREVENTIVE'
      ExecDate = $Today.AddDays(-20).ToString('yyyy-MM-dd')
      NextDate = $Today.AddDays(60).ToString('yyyy-MM-dd')
      Mileage = 45200
      Cost = 1250.00
      Notes = 'Mantenimiento preventivo al dia'
    },
    @{
      Key = 'DueSoon'
      Plate = 'RIN-102'
      Brand = 'Scania'
      Model = 'R450'
      Year = 2022
      CapacityKg = 24000.00
      MaintType = 'PREVENTIVE'
      ExecDate = $Today.AddDays(-87).ToString('yyyy-MM-dd')
      NextDate = $Today.AddDays(3).ToString('yyyy-MM-dd')
      Mileage = 78400
      Cost = 980.00
      Notes = 'Proximo cambio de aceite y revision de frenos'
    },
    @{
      Key = 'Overdue'
      Plate = 'RIN-103'
      Brand = 'Mercedes-Benz'
      Model = 'Actros 2645'
      Year = 2021
      CapacityKg = 26000.00
      MaintType = 'CORRECTIVE'
      ExecDate = $Today.AddDays(-95).ToString('yyyy-MM-dd')
      NextDate = $Today.AddDays(-5).ToString('yyyy-MM-dd')
      Mileage = 112500
      Cost = 1640.00
      Notes = 'Mantenimiento vencido pendiente de ingreso a taller'
    }
  )

  $vehicles = @{}
  $vehiclesCreated = 0
  $maintenancesCreated = 0

  foreach ($spec in $vehicleSpecs) {
    $v = $existingVehicles | Where-Object { $_.plateNumber -eq $spec.Plate } | Select-Object -First 1
    if ($null -eq $v) {
      $created = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path '/api/v1/vehicles' -Token $opsToken `
        -Body @{
          plateNumber       = $spec.Plate
          brand             = $spec.Brand
          model             = $spec.Model
          modelYear         = $spec.Year
          payloadCapacityKg = $spec.CapacityKg
        } `
        -ExpectedStatus @(201)
      $v = $created.Data
      $vehiclesCreated++
    }

    $health = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path "/api/v1/vehicles/$($v.id)/health-status" `
      -Token $opsToken -ExpectedStatus @(200)).Data
    if ($null -eq $health.nextMaintenanceDate) {
      $null = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path "/api/v1/vehicles/$($v.id)/maintenances" `
        -Token $opsToken `
        -Body @{
          vehicleId           = $v.id
          maintenanceType     = $spec.MaintType
          executionDate       = $spec.ExecDate
          mileage             = $spec.Mileage
          cost                = $spec.Cost
          nextMaintenanceDate = $spec.NextDate
          notes               = $spec.Notes
        } `
        -ExpectedStatus @(201)
      $maintenancesCreated++
    }
    $vehicles[$spec.Key] = $v
  }

  $driversRes = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path '/api/v1/drivers' -Token $opsToken -ExpectedStatus @(200)
  $existingDrivers = @($driversRes.Data)

  $driverSpecs = @(
    @{
      Key = 'Driver1'
      UserId = [string]$Auth.Driver1User.id
      FullName = $Config.Driver1Name
      DocType = 'DNI'
      DocNumber = '44556671'
      License = 'Q44556671'
      Category = 'AIIIC'
    },
    @{
      Key = 'Driver2'
      UserId = [string]$Auth.Driver2User.id
      FullName = $Config.Driver2Name
      DocType = 'DNI'
      DocNumber = '44556672'
      License = 'Q44556672'
      Category = 'AIIIC'
    }
  )

  $drivers = @{}
  $driversCreated = 0
  $expDate = $Today.AddDays(365).ToString('yyyy-MM-dd')

  foreach ($spec in $driverSpecs) {
    $d = $existingDrivers | Where-Object {
      ($_.userId -and [string]$_.userId -eq $spec.UserId) -or
      ($_.documentNumber -eq $spec.DocNumber) -or
      ($_.licenseNumber -eq $spec.License)
    } | Select-Object -First 1

    if ($null -eq $d) {
      $created = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path '/api/v1/drivers' -Token $opsToken `
        -Body @{
          userId                = $spec.UserId
          fullName              = $spec.FullName
          documentType          = $spec.DocType
          documentNumber        = $spec.DocNumber
          licenseNumber         = $spec.License
          licenseCategory       = $spec.Category
          licenseExpirationDate = $expDate
        } `
        -ExpectedStatus @(201)
      $d = $created.Data
      $driversCreated++
    } elseif ([string]$d.userId -ne $spec.UserId) {
      $d = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'PUT' -Path "/api/v1/drivers/$($d.id)/account" `
        -Token $opsToken -Body @{ userId = $spec.UserId } -ExpectedStatus @(200)).Data
    }
    $drivers[$spec.Key] = $d
  }

  return [pscustomobject]@{
    Vehicles            = $vehicles
    Drivers             = $drivers
    VehiclesCreated     = $vehiclesCreated
    MaintenancesCreated = $maintenancesCreated
    DriversCreated      = $driversCreated
  }
}

function Ensure-DemoExpense {
  <# Crea un gasto idempotente por idempotencyKey, adjunta PNG si corresponde y aplica el estado objetivo. #>
  param(
    [Parameter(Mandatory)][string]$GatewayUrl,
    [Parameter(Mandatory)][string]$TripId,
    [Parameter(Mandatory)][string]$DriverToken,
    [Parameter(Mandatory)][string]$OpsToken,
    [Parameter(Mandatory)][hashtable]$Spec,
    [Parameter(Mandatory)][byte[]]$PngBytes,
    [Parameter(Mandatory)]$Counters
  )

  $existingList = @((Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path "/api/v1/trips/$TripId/expenses" `
    -Token $OpsToken -ExpectedStatus @(200)).Data)
  $expense = $existingList | Where-Object { $_.idempotencyKey -eq $Spec.IdempotencyKey } | Select-Object -First 1

  if ($null -eq $expense) {
    $expense = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path "/api/v1/trips/$TripId/expenses" `
      -Token $DriverToken `
      -Body @{
        category       = $Spec.Category
        amount         = $Spec.Amount
        currency       = 'PEN'
        expenseDate    = $Spec.ExpenseDate
        idempotencyKey = $Spec.IdempotencyKey
      } `
      -ExpectedStatus @(201)).Data
    $Counters.ExpensesCreated++
  }

  if ($Spec.AttachEvidence -and $null -eq $expense.evidence -and -not $expense.isLocked) {
    $expense = (Invoke-RindeMultipartPng -GatewayUrl $GatewayUrl `
      -Path "/api/v1/expenses/$($expense.id)/evidences" `
      -Token $DriverToken -PngBytes $PngBytes -FileName $Spec.FileName).Data
    $Counters.EvidencesUploaded++
  }

  if ($Spec.TargetStatus -in @('APPROVED', 'OBSERVED') -and $expense.status -ne $Spec.TargetStatus -and -not $expense.isLocked) {
    $body = @{ status = $Spec.TargetStatus }
    if ($Spec.TargetStatus -eq 'OBSERVED') {
      $body['reason'] = $Spec.ObservationReason
    }
    $expense = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'PATCH' -Path "/api/v1/expenses/$($expense.id)/status" `
      -Token $OpsToken -Body $body -ExpectedStatus @(200)).Data
  }

  return $expense
}

function Ensure-DemoIncident {
  <# Busca incidencia por tipo y descripción en el viaje, la reporta si está en ruta y la cierra si aplica. #>
  param(
    [Parameter(Mandatory)][string]$GatewayUrl,
    [Parameter(Mandatory)][string]$TripId,
    [Parameter(Mandatory)][string]$DriverToken,
    [Parameter(Mandatory)][string]$OpsToken,
    [Parameter(Mandatory)][hashtable]$Spec,
    [Parameter(Mandatory)]$Counters,
    [bool]$AllowCreate = $true
  )

  $existingList = @((Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path "/api/v1/incidents?tripId=$TripId" `
    -Token $OpsToken -ExpectedStatus @(200)).Data)
  $incident = $existingList | Where-Object {
    $_.type -eq $Spec.Type -and $_.description -eq $Spec.Description
  } | Select-Object -First 1

  if ($null -eq $incident -and $AllowCreate) {
    $res = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path "/api/v1/trips/$TripId/incidents" `
      -Token $DriverToken `
      -Body @{
        type           = $Spec.Type
        description    = $Spec.Description
        evidenceUrl    = $null
        idempotencyKey = $Spec.IdempotencyKey
      } `
      -ExpectedStatus @(200, 201)
    $incident = $res.Data
    if ($res.StatusCode -eq 201) {
      $Counters.IncidentsCreated++
    }
  }

  if ($null -ne $incident -and $Spec.TargetStatus -eq 'CLOSED' -and $incident.status -ne 'CLOSED') {
    $incident = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path "/api/v1/incidents/$($incident.id)/close" `
      -Token $OpsToken -Body @{ action = $Spec.CloseAction } -ExpectedStatus @(200)).Data
    $Counters.IncidentsClosed++
  }

  return $incident
}

function Ensure-DemoTripAndAdvance {
  <# Crea o reutiliza un viaje por origen/destino, lo asigna si aplica y registra su adelanto sin duplicar. #>
  param(
    [Parameter(Mandatory)][string]$GatewayUrl,
    [Parameter(Mandatory)][string]$OpsToken,
    [Parameter(Mandatory)][AllowEmptyCollection()][array]$ExistingTrips,
    [Parameter(Mandatory)][hashtable]$TripSpec,
    [Parameter(Mandatory)]$Counters
  )

  $trip = $ExistingTrips | Where-Object {
    $_.origin -eq $TripSpec.Origin -and $_.destination -eq $TripSpec.Destination
  } | Select-Object -First 1

  if ($null -eq $trip) {
    $trip = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path '/api/v1/trips' -Token $OpsToken `
      -Body @{
        origin           = $TripSpec.Origin
        destination      = $TripSpec.Destination
        cargoDescription = $TripSpec.CargoDescription
        cargoWeightKg    = $TripSpec.CargoWeightKg
        departureDate    = $TripSpec.DepartureDate
        confirmPastDate  = $TripSpec.ConfirmPastDate
      } `
      -ExpectedStatus @(201)).Data
    $Counters.TripsCreated++
  }

  if ($TripSpec.TargetStatus -ne 'SCHEDULED' -and $trip.status -eq 'SCHEDULED') {
    $null = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'PUT' -Path "/api/v1/trips/$($trip.id)/assignment" `
      -Token $OpsToken `
      -Body @{
        vehicleId                 = $TripSpec.VehicleId
        driverId                  = $TripSpec.DriverId
        confirmOverdueMaintenance = $TripSpec.ConfirmOverdue
      } `
      -ExpectedStatus @(200)
    $trip = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path "/api/v1/trips/$($trip.id)" `
      -Token $OpsToken -ExpectedStatus @(200)).Data
  }

  if ($null -ne $TripSpec.Advance -and $trip.status -in @('SCHEDULED', 'ASSIGNED')) {
    $advances = @((Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path "/api/v1/trips/$($trip.id)/advances" `
      -Token $OpsToken -ExpectedStatus @(200)).Data)
    if ($advances.Count -eq 0) {
      $null = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path "/api/v1/trips/$($trip.id)/advances" `
        -Token $OpsToken `
        -Body @{
          amount         = $TripSpec.Advance.Amount
          currency       = 'PEN'
          deliveryMethod = $TripSpec.Advance.Method
        } `
        -ExpectedStatus @(201)
      $Counters.AdvancesCreated++
    }
  }

  return $trip
}

function Invoke-DemoOperations {
  <# Orquesta viajes en los 5 estados, adelantos, gastos con comprobante PNG, incidencias y liquidaciones. #>
  param(
    [Parameter(Mandatory)][string]$GatewayUrl,
    [Parameter(Mandatory)]$Auth,
    [Parameter(Mandatory)]$Fleet,
    [Parameter(Mandatory)]$Config,
    [Parameter(Mandatory)][datetime]$Today
  )

  $opsToken = $Auth.OpsToken
  $d1Token = $Auth.Driver1Token
  $d2Token = $Auth.Driver2Token
  $ruc = $Config.CompanyRuc
  $pngBytes = New-DemoPngBytes

  $counters = [pscustomobject]@{
    TripsCreated      = 0
    AdvancesCreated   = 0
    ExpensesCreated   = 0
    EvidencesUploaded = 0
    IncidentsCreated  = 0
    IncidentsClosed   = 0
    SettlementsClosed = 0
  }

  $existingTrips = @((Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path '/api/v1/trips' -Token $opsToken -ExpectedStatus @(200)).Data)
  $vOk = $Fleet.Vehicles['Ok']
  $vDue = $Fleet.Vehicles['DueSoon']
  $vOver = $Fleet.Vehicles['Overdue']
  $d1 = $Fleet.Drivers['Driver1']
  $d2 = $Fleet.Drivers['Driver2']

  $trip5Spec = @{
    Origin = 'Lima'; Destination = 'Chiclayo'
    CargoDescription = 'Productos alimenticios no perecibles'; CargoWeightKg = 5400.00
    DepartureDate = $Today.AddDays(-5).ToString('yyyy-MM-dd'); ConfirmPastDate = $true
    TargetStatus = 'SETTLED'; VehicleId = $vOk.id; DriverId = $d1.id; ConfirmOverdue = $false
    Advance = @{ Amount = 600.00; Method = 'TRANSFER' }
  }
  $trip5 = Ensure-DemoTripAndAdvance -GatewayUrl $GatewayUrl -OpsToken $opsToken -ExistingTrips $existingTrips -TripSpec $trip5Spec -Counters $counters
  if ($trip5.status -eq 'ASSIGNED') {
    $trip5 = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path "/api/v1/trips/$($trip5.id)/start" -Token $d1Token -ExpectedStatus @(200)).Data
  }
  if ($trip5.status -eq 'IN_ROUTE') {
    $null = Ensure-DemoIncident -GatewayUrl $GatewayUrl -TripId $trip5.id -DriverToken $d1Token -OpsToken $opsToken -Counters $counters -Spec @{
      IdempotencyKey = "DEMO-$ruc-INC-03"; Type = 'DELAY'
      Description = 'Congestion vehicular en peaje de Chimbote'; TargetStatus = 'CLOSED'
      CloseAction = 'Se reprogramo la ventana de descarga con el almacen de destino'
    }
    $trip5 = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path "/api/v1/trips/$($trip5.id)/finish" -Token $d1Token -ExpectedStatus @(200)).Data
  } else {
    $null = Ensure-DemoIncident -GatewayUrl $GatewayUrl -TripId $trip5.id -DriverToken $d1Token -OpsToken $opsToken -Counters $counters -AllowCreate $false -Spec @{
      IdempotencyKey = "DEMO-$ruc-INC-03"; Type = 'DELAY'
      Description = 'Congestion vehicular en peaje de Chimbote'; TargetStatus = 'CLOSED'
      CloseAction = 'Se reprogramo la ventana de descarga con el almacen de destino'
    }
  }
  foreach ($expSpec in @(
    @{ IdempotencyKey = "DEMO-$ruc-EXP-05"; Category = 'FUEL'; Amount = 320.00; ExpenseDate = $Today.AddDays(-5).ToString('yyyy-MM-dd'); AttachEvidence = $true; FileName = 'combustible-chiclayo.png'; TargetStatus = 'APPROVED' },
    @{ IdempotencyKey = "DEMO-$ruc-EXP-06"; Category = 'TOLL'; Amount = 48.50; ExpenseDate = $Today.AddDays(-5).ToString('yyyy-MM-dd'); AttachEvidence = $true; FileName = 'peaje-chiclayo.png'; TargetStatus = 'APPROVED' },
    @{ IdempotencyKey = "DEMO-$ruc-EXP-07"; Category = 'OTHER'; Amount = 35.00; ExpenseDate = $Today.AddDays(-5).ToString('yyyy-MM-dd'); AttachEvidence = $true; FileName = 'otro-chiclayo.png'; TargetStatus = 'OBSERVED'; ObservationReason = 'Boleta sin detalle de concepto operativo' }
  )) {
    $null = Ensure-DemoExpense -GatewayUrl $GatewayUrl -TripId $trip5.id -DriverToken $d1Token -OpsToken $opsToken -Spec $expSpec -PngBytes $pngBytes -Counters $counters
  }
  $settlement5 = Wait-UntilCondition -TimeoutMessage "No se abrió la liquidación del viaje $($trip5.code) a tiempo." -Probe {
    $res = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path "/api/v1/trips/$($trip5.id)/settlement" -Token $opsToken -ExpectedStatus @(200, 404)
    if ($res.StatusCode -eq 200) { return $res.Data }
    return $null
  }
  if ($settlement5.status -eq 'OPEN') {
    $settlement5 = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path "/api/v1/settlements/$($settlement5.id)/close" -Token $opsToken -ExpectedStatus @(200)).Data
    $counters.SettlementsClosed++
  }
  if ($trip5.status -ne 'SETTLED') {
    $trip5 = Wait-UntilCondition -TimeoutMessage "El viaje $($trip5.code) no pasó a estado SETTLED tras cerrar su liquidación." -Probe {
      $t = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path "/api/v1/trips/$($trip5.id)" -Token $opsToken -ExpectedStatus @(200)).Data
      if ($t.status -eq 'SETTLED') { return $t }
      return $null
    }
  }

  $trip4Spec = @{
    Origin = 'Arequipa'; Destination = 'Cusco'
    CargoDescription = 'Repuestos automotrices'; CargoWeightKg = 2900.00
    DepartureDate = $Today.AddDays(-2).ToString('yyyy-MM-dd'); ConfirmPastDate = $true
    TargetStatus = 'FINISHED'; VehicleId = $vOver.id; DriverId = $d2.id; ConfirmOverdue = $true
    Advance = @{ Amount = 420.00; Method = 'CASH' }
  }
  $trip4 = Ensure-DemoTripAndAdvance -GatewayUrl $GatewayUrl -OpsToken $opsToken -ExistingTrips $existingTrips -TripSpec $trip4Spec -Counters $counters
  if ($trip4.status -eq 'ASSIGNED') {
    $trip4 = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path "/api/v1/trips/$($trip4.id)/start" -Token $d2Token -ExpectedStatus @(200)).Data
  }
  if ($trip4.status -eq 'IN_ROUTE') {
    $null = Ensure-DemoIncident -GatewayUrl $GatewayUrl -TripId $trip4.id -DriverToken $d2Token -OpsToken $opsToken -Counters $counters -Spec @{
      IdempotencyKey = "DEMO-$ruc-INC-02"; Type = 'BREAKDOWN'
      Description = 'Desgaste en neumatico posterior derecho antes de Sicuani'; TargetStatus = 'CLOSED'
      CloseAction = 'Se reemplazo por neumatico de repuesto y se programo ingreso a taller'
    }
    $trip4 = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path "/api/v1/trips/$($trip4.id)/finish" -Token $d2Token -ExpectedStatus @(200)).Data
  } else {
    $null = Ensure-DemoIncident -GatewayUrl $GatewayUrl -TripId $trip4.id -DriverToken $d2Token -OpsToken $opsToken -Counters $counters -AllowCreate $false -Spec @{
      IdempotencyKey = "DEMO-$ruc-INC-02"; Type = 'BREAKDOWN'
      Description = 'Desgaste en neumatico posterior derecho antes de Sicuani'; TargetStatus = 'CLOSED'
      CloseAction = 'Se reemplazo por neumatico de repuesto y se programo ingreso a taller'
    }
  }
  foreach ($expSpec in @(
    @{ IdempotencyKey = "DEMO-$ruc-EXP-03"; Category = 'FUEL'; Amount = 210.00; ExpenseDate = $Today.AddDays(-2).ToString('yyyy-MM-dd'); AttachEvidence = $true; FileName = 'combustible-cusco.png'; TargetStatus = 'APPROVED' },
    @{ IdempotencyKey = "DEMO-$ruc-EXP-04"; Category = 'LODGING'; Amount = 95.00; ExpenseDate = $Today.AddDays(-2).ToString('yyyy-MM-dd'); AttachEvidence = $true; FileName = 'hospedaje-cusco.png'; TargetStatus = 'REGISTERED' }
  )) {
    $null = Ensure-DemoExpense -GatewayUrl $GatewayUrl -TripId $trip4.id -DriverToken $d2Token -OpsToken $opsToken -Spec $expSpec -PngBytes $pngBytes -Counters $counters
  }
  $settlement4 = Wait-UntilCondition -TimeoutMessage "No se abrió la liquidación del viaje $($trip4.code) a tiempo." -Probe {
    $res = Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path "/api/v1/trips/$($trip4.id)/settlement" -Token $opsToken -ExpectedStatus @(200, 404)
    if ($res.StatusCode -eq 200) { return $res.Data }
    return $null
  }

  $trip3Spec = @{
    Origin = 'Lima'; Destination = 'Trujillo'
    CargoDescription = 'Materiales de construccion'; CargoWeightKg = 6800.00
    DepartureDate = $Today.ToString('yyyy-MM-dd'); ConfirmPastDate = $false
    TargetStatus = 'IN_ROUTE'; VehicleId = $vOk.id; DriverId = $d1.id; ConfirmOverdue = $false
    Advance = @{ Amount = 500.00; Method = 'TRANSFER' }
  }
  $trip3 = Ensure-DemoTripAndAdvance -GatewayUrl $GatewayUrl -OpsToken $opsToken -ExistingTrips $existingTrips -TripSpec $trip3Spec -Counters $counters
  if ($trip3.status -eq 'ASSIGNED') {
    $trip3 = (Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'POST' -Path "/api/v1/trips/$($trip3.id)/start" -Token $d1Token -ExpectedStatus @(200)).Data
  }
  foreach ($expSpec in @(
    @{ IdempotencyKey = "DEMO-$ruc-EXP-01"; Category = 'FUEL'; Amount = 180.50; ExpenseDate = $Today.ToString('yyyy-MM-dd'); AttachEvidence = $true; FileName = 'combustible-trujillo.png'; TargetStatus = 'REGISTERED' },
    @{ IdempotencyKey = "DEMO-$ruc-EXP-02"; Category = 'FOOD'; Amount = 45.00; ExpenseDate = $Today.ToString('yyyy-MM-dd'); AttachEvidence = $false; FileName = ''; TargetStatus = 'PENDING_SUPPORT' }
  )) {
    $null = Ensure-DemoExpense -GatewayUrl $GatewayUrl -TripId $trip3.id -DriverToken $d1Token -OpsToken $opsToken -Spec $expSpec -PngBytes $pngBytes -Counters $counters
  }
  $null = Ensure-DemoIncident -GatewayUrl $GatewayUrl -TripId $trip3.id -DriverToken $d1Token -OpsToken $opsToken -Counters $counters -Spec @{
    IdempotencyKey = "DEMO-$ruc-INC-01"; Type = 'DELAY'
    Description = 'Desvio por mantenimiento vial en la Panamericana Norte'; TargetStatus = 'OPEN'
  }

  $trip2Spec = @{
    Origin = 'Lima'; Destination = 'Huancayo'
    CargoDescription = 'Insumos agricolas empaquetados'; CargoWeightKg = 4500.00
    DepartureDate = $Today.AddDays(2).ToString('yyyy-MM-dd'); ConfirmPastDate = $false
    TargetStatus = 'ASSIGNED'; VehicleId = $vDue.id; DriverId = $d2.id; ConfirmOverdue = $false
    Advance = @{ Amount = 350.00; Method = 'CASH' }
  }
  $trip2 = Ensure-DemoTripAndAdvance -GatewayUrl $GatewayUrl -OpsToken $opsToken -ExistingTrips $existingTrips -TripSpec $trip2Spec -Counters $counters

  $trip1Spec = @{
    Origin = 'Lima'; Destination = 'Ica'
    CargoDescription = 'Equipos de refrigeracion industrial'; CargoWeightKg = 3200.00
    DepartureDate = $Today.AddDays(4).ToString('yyyy-MM-dd'); ConfirmPastDate = $false
    TargetStatus = 'SCHEDULED'; VehicleId = $null; DriverId = $null; ConfirmOverdue = $false
    Advance = $null
  }
  $trip1 = Ensure-DemoTripAndAdvance -GatewayUrl $GatewayUrl -OpsToken $opsToken -ExistingTrips $existingTrips -TripSpec $trip1Spec -Counters $counters

  $finalTrips = @((Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path '/api/v1/trips' -Token $opsToken -ExpectedStatus @(200)).Data)
  $finalIncidents = @((Invoke-RindeApi -GatewayUrl $GatewayUrl -Method 'GET' -Path '/api/v1/incidents' -Token $opsToken -ExpectedStatus @(200)).Data)

  return [pscustomobject]@{
    Counters         = $counters
    Trips            = @($trip1, $trip2, $trip3, $trip4, $trip5)
    TotalTrips       = $finalTrips.Count
    TotalIncidents   = $finalIncidents.Count
    OpenSettlementId = $settlement4.id
    ClosedSettlementId = $settlement5.id
  }
}

function Format-DemoSummaryLines {
  <# Construye el resumen legible de salida sin incluir contraseñas ni tokens JWT. #>
  param(
    [Parameter(Mandatory)]$Config,
    [Parameter(Mandatory)]$Auth,
    [Parameter(Mandatory)]$Fleet,
    [Parameter(Mandatory)]$Operations
  )

  $lines = [System.Collections.Generic.List[string]]::new()
  $lines.Add('============================================================')
  $lines.Add('RINDE by NEXUS: Sembrado de demostración completado')
  $lines.Add('============================================================')
  $lines.Add("Gateway URL: $($Config.GatewayUrl)")
  $lines.Add("Empresa: $($Config.CompanyTradeName) (RUC $($Config.CompanyRuc))")
  $lines.Add("Empresa verificada en esta ejecución: $($Auth.TenantVerified)")
  $lines.Add('Cuentas activas:')
  $lines.Add("  - ADMINISTRATOR: $($Config.AdminEmail)")
  $lines.Add("  - OPERATIONS_MANAGER: $($Config.OpsEmail)")
  $lines.Add("  - DRIVER 1: $($Config.Driver1Email)")
  $lines.Add("  - DRIVER 2: $($Config.Driver2Email)")
  $lines.Add("Flota: $($Fleet.Vehicles.Count) vehículos demo ($($Fleet.VehiclesCreated) creados, $($Fleet.MaintenancesCreated) mantenimientos registrados), $($Fleet.Drivers.Count) conductores ($($Fleet.DriversCreated) creados)")
  $lines.Add("Viajes totales en empresa: $($Operations.TotalTrips) ($($Operations.Counters.TripsCreated) creados; estados SCHEDULED, ASSIGNED, IN_ROUTE, FINISHED, SETTLED)")
  $lines.Add("Adelantos creados en esta ejecución: $($Operations.Counters.AdvancesCreated) (CASH y TRANSFER)")
  $lines.Add("Gastos creados en esta ejecución: $($Operations.Counters.ExpensesCreated) ($($Operations.Counters.EvidencesUploaded) comprobantes PNG subidos)")
  $lines.Add("Incidencias totales en empresa: $($Operations.TotalIncidents) ($($Operations.Counters.IncidentsCreated) creadas, $($Operations.Counters.IncidentsClosed) cerradas)")
  $lines.Add("Liquidaciones demo: OPEN=$($Operations.OpenSettlementId), CLOSED=$($Operations.ClosedSettlementId)")
  $lines.Add('============================================================')
  return $lines
}

function Invoke-DemoSeed {
  <# Punto de entrada principal del script de sembrado de datos de demostración. #>
  param(
    [string]$GatewayUrl = '',
    [string]$EnvFile = ''
  )

  $config = Resolve-DemoConfiguration -GatewayUrlParam $GatewayUrl -EnvFileParam $EnvFile
  $today = [DateTime]::UtcNow.Date

  $auth = Ensure-DemoTenantAndUsers -Config $config
  $fleet = Ensure-DemoFleet -GatewayUrl $config.GatewayUrl -Auth $auth -Config $config -Today $today
  $ops = Invoke-DemoOperations -GatewayUrl $config.GatewayUrl -Auth $auth -Fleet $fleet -Config $config -Today $today

  $summary = Format-DemoSummaryLines -Config $config -Auth $auth -Fleet $fleet -Operations $ops

  return [pscustomobject]@{
    Config       = [pscustomobject]@{ GatewayUrl = $config.GatewayUrl; CompanyRuc = $config.CompanyRuc; CompanyTradeName = $config.CompanyTradeName }
    Auth         = [pscustomobject]@{ TenantId = $auth.TenantId; TenantCreated = $auth.TenantCreated; TenantVerified = $auth.TenantVerified; UsersCreated = $auth.UsersCreated; UsersActivated = $auth.UsersActivated }
    Fleet        = $fleet
    Operations   = $ops
    SummaryLines = @($summary)
  }
}

if ($MyInvocation.InvocationName -ne '.') {
  $seedResult = Invoke-DemoSeed -GatewayUrl $GatewayUrl -EnvFile $EnvFile
  $seedResult.SummaryLines | ForEach-Object { Write-Output $_ }
}
