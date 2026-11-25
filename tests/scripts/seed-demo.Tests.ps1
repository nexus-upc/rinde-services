#requires -Version 7.2
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
$scriptPath = Join-Path $repoRoot 'scripts/seed-demo.ps1'

function Assert-True {
  param([bool]$Condition, [string]$Message)
  if (-not $Condition) {
    throw "Aserción fallida: $Message"
  }
}

function Assert-Equal {
  param($Actual, $Expected, [string]$Message)
  if ($Actual -ne $Expected) {
    throw "Aserción fallida ($Message): esperado '$Expected', obtenido '$Actual'."
  }
}

function Assert-ThrowsContaining {
  param([scriptblock]$Action, [string]$ExpectedSubstring, [string]$Message)
  $threw = $false
  $errMsg = ''
  try {
    & $Action
  } catch {
    $threw = $true
    $errMsg = $_.Exception.Message
  }
  Assert-True $threw "Se esperaba excepción ($Message)"
  Assert-True ($errMsg -like "*$ExpectedSubstring*") "Mensaje inesperado ($Message): '$errMsg' no contiene '$ExpectedSubstring'"
}

function Get-FreeTcpPort {
  $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
  $listener.Start()
  $port = ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
  $listener.Stop()
  return $port
}

function Start-MockGatewayServer {
  param(
    [bool]$MailboxEnabled = $true,
    [int]$TenantCurrentStatus = 200
  )

  $port = Get-FreeTcpPort
  $baseUrl = "http://127.0.0.1:$port"

  $state = [hashtable]::Synchronized(@{
    MailboxEnabled      = $MailboxEnabled
    TenantCurrentStatus = $TenantCurrentStatus
    Tenant              = $null
    Users          = [System.Collections.ArrayList]::Synchronized([System.Collections.ArrayList]::new())
    Mailbox        = [System.Collections.ArrayList]::Synchronized([System.Collections.ArrayList]::new())
    Vehicles       = [System.Collections.ArrayList]::Synchronized([System.Collections.ArrayList]::new())
    Maintenances   = [System.Collections.ArrayList]::Synchronized([System.Collections.ArrayList]::new())
    Drivers        = [System.Collections.ArrayList]::Synchronized([System.Collections.ArrayList]::new())
    Trips          = [System.Collections.ArrayList]::Synchronized([System.Collections.ArrayList]::new())
    Advances       = [System.Collections.ArrayList]::Synchronized([System.Collections.ArrayList]::new())
    Expenses       = [System.Collections.ArrayList]::Synchronized([System.Collections.ArrayList]::new())
    Incidents      = [System.Collections.ArrayList]::Synchronized([System.Collections.ArrayList]::new())
    Settlements    = [System.Collections.ArrayList]::Synchronized([System.Collections.ArrayList]::new())
    TripSeq        = 0
    StopRequested  = $false
  })

  $http = [System.Net.HttpListener]::new()
  $http.Prefixes.Add("$baseUrl/")
  $http.Start()

  $runspace = [runspacefactory]::CreateRunspace()
  $runspace.Open()
  $runspace.SessionStateProxy.SetVariable('Listener', $http)
  $runspace.SessionStateProxy.SetVariable('State', $state)

  $ps = [powershell]::Create()
  $ps.Runspace = $runspace
  $null = $ps.AddScript({
    function Send-Json {
      param($Ctx, [int]$Status, $Obj)
      $Ctx.Response.StatusCode = $Status
      $Ctx.Response.ContentType = 'application/json; charset=utf-8'
      if ($null -ne $Obj) {
        if ($Obj -is [System.Collections.IEnumerable] -and -not ($Obj -is [string]) -and -not ($Obj -is [System.Collections.IDictionary])) {
          $json = ConvertTo-Json -InputObject @($Obj) -Depth 20 -Compress
        } else {
          $json = ConvertTo-Json -InputObject $Obj -Depth 20 -Compress
        }
        $bytes = [Text.Encoding]::UTF8.GetBytes($json)
        $Ctx.Response.OutputStream.Write($bytes, 0, $bytes.Length)
      }
      $Ctx.Response.OutputStream.Close()
    }

    function Read-ReqBytes {
      param($Ctx)
      $ms = [IO.MemoryStream]::new()
      $Ctx.Request.InputStream.CopyTo($ms)
      return $ms.ToArray()
    }

    function Parse-Auth {
      param($Ctx)
      $auth = $Ctx.Request.Headers['Authorization']
      if ([string]::IsNullOrWhiteSpace($auth) -or -not $auth.StartsWith('Bearer ')) { return $null }
      $raw = $auth.Substring(7).Trim()
      if ($raw -eq 'platform-token') {
        return [pscustomobject]@{ IsPlatform = $true; UserId = $null; Role = 'PLATFORM_OWNER'; Email = 'owner@example.invalid' }
      }
      if ($raw.StartsWith('user:')) {
        $parts = $raw.Split(':', 4)
        return [pscustomobject]@{ IsPlatform = $false; UserId = $parts[1]; Role = $parts[2]; Email = $parts[3] }
      }
      return $null
    }

    while (-not $State.StopRequested -and $Listener.IsListening) {
      try {
        $ctx = $Listener.GetContext()
      } catch {
        break
      }

      try {
        $method = $ctx.Request.HttpMethod
        $path = $ctx.Request.Url.AbsolutePath
        $query = $ctx.Request.Url.Query
        $caller = Parse-Auth -Ctx $ctx

        if ($method -eq 'POST' -and $path -eq '/api/v1/platform/auth/sign-in') {
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          if ($body.email -eq 'owner@example.invalid') {
            Send-Json $ctx 200 @{ accessToken = 'platform-token'; tokenType = 'Bearer'; expiresInSeconds = 900 }
          } else {
            Send-Json $ctx 401 @{ status = 401; detail = 'Credenciales inválidas.' }
          }
          continue
        }

        if ($method -eq 'GET' -and $path -eq '/api/v1/platform/demo-mailbox/messages') {
          if (-not $State.MailboxEnabled) { Send-Json $ctx 404 @{ status = 404; detail = 'Not Found' }; continue }
          if ($null -eq $caller -or -not $caller.IsPlatform) { Send-Json $ctx 401 @{ status = 401; detail = 'Token requerido.' }; continue }
          $items = @($State.Mailbox | ForEach-Object {
            @{ id = $_.id; recipient = $_.recipient; purpose = $_.purpose; state = $_.state; createdAt = $_.createdAt }
          })
          Send-Json $ctx 200 @{ items = $items; page = 0; size = 20; total = $items.Count }
          continue
        }

        if ($method -eq 'GET' -and $path -match '^/api/v1/platform/demo-mailbox/messages/([^/]+)$') {
          if (-not $State.MailboxEnabled) { Send-Json $ctx 404 @{ status = 404; detail = 'Not Found' }; continue }
          $msgId = $Matches[1]
          $msg = $State.Mailbox | Where-Object { $_.id -eq $msgId } | Select-Object -First 1
          if ($null -eq $msg) { Send-Json $ctx 404 @{ status = 404; detail = 'El mensaje no existe.' }; continue }
          Send-Json $ctx 200 @{ message = @{ id = $msg.id; recipient = $msg.recipient; purpose = $msg.purpose; state = $msg.state }; actionPath = $msg.actionPath }
          continue
        }

        if ($method -eq 'POST' -and $path -eq '/api/v1/tenants') {
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          if ($null -ne $State.Tenant) {
            Send-Json $ctx 409 @{ status = 409; detail = 'El RUC o el correo ya están registrados.' }
            continue
          }
          $tid = [guid]::NewGuid().ToString()
          $uid = [guid]::NewGuid().ToString()
          $rawToken = 'verif+tok/1=='
          $State.Tenant = @{
            id = $tid; tradeName = $body.tradeName; ruc = $body.ruc; status = 'PENDING_VERIFICATION'
            phone = $null; address = $null; verifyToken = $rawToken
          }
          $null = $State.Users.Add(@{
            id = $uid; fullName = $body.administratorFullName; email = $body.administratorEmail.ToLowerInvariant()
            role = 'ADMINISTRATOR'; status = 'ACTIVE'; password = $body.password; inviteToken = $null
          })
          $null = $State.Mailbox.Add(@{
            id = [guid]::NewGuid().ToString(); recipient = $body.administratorEmail.ToLowerInvariant()
            purpose = 'EMAIL_VERIFICATION'; state = 'ACTIVE'; createdAt = [DateTime]::UtcNow.ToString('o')
            actionPath = "/verify-email?tenant=$tid&token=$([Uri]::EscapeDataString($rawToken))"
          })
          Send-Json $ctx 201 @{ id = $tid; tradeName = $body.tradeName; ruc = $body.ruc; status = 'PENDING_VERIFICATION' }
          continue
        }

        if ($method -eq 'POST' -and $path -match '^/api/v1/tenants/([^/]+)/verification$') {
          $tid = $Matches[1]
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          if ($null -eq $State.Tenant -or $State.Tenant.id -ne $tid) { Send-Json $ctx 404 @{ status = 404; detail = 'La empresa no existe.' }; continue }
          if ($body.token -ne $State.Tenant.verifyToken) { Send-Json $ctx 400 @{ status = 400; detail = 'Token inválido.' }; continue }
          $State.Tenant.status = 'ACTIVE'
          Send-Json $ctx 200 @{ id = $State.Tenant.id; tradeName = $State.Tenant.tradeName; ruc = $State.Tenant.ruc; status = 'ACTIVE' }
          continue
        }

        if ($method -eq 'POST' -and $path -eq '/api/v1/auth/sign-in') {
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          $email = $body.email.ToLowerInvariant()
          $u = $State.Users | Where-Object { $_.email -eq $email } | Select-Object -First 1
          if ($null -eq $u -or $u.status -ne 'ACTIVE' -or $u.password -ne $body.password) {
            Send-Json $ctx 401 @{ status = 401; detail = 'El correo o la contraseña son incorrectos.' }
            continue
          }
          if ($State.Tenant.status -eq 'PENDING_VERIFICATION') {
            Send-Json $ctx 403 @{ status = 403; detail = 'La empresa aún no confirmó su correo.' }
            continue
          }
          Send-Json $ctx 200 @{ accessToken = "user:$($u.id):$($u.role):$($u.email)"; tokenType = 'Bearer'; expiresInSeconds = 28800 }
          continue
        }

        if ($method -eq 'POST' -and $path -eq '/api/v1/auth/password') {
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          $u = $State.Users | Where-Object { $_.inviteToken -eq $body.token } | Select-Object -First 1
          if ($null -eq $u) { Send-Json $ctx 400 @{ status = 400; detail = 'Enlace inválido.' }; continue }
          $u.password = $body.password
          $u.status = 'ACTIVE'
          $u.inviteToken = $null
          Send-Json $ctx 204 $null
          continue
        }

        if ($null -eq $caller) { Send-Json $ctx 401 @{ status = 401; detail = 'Token requerido.' }; continue }

        if ($method -eq 'GET' -and $path -eq '/api/v1/tenants/current') {
          if ($State.TenantCurrentStatus -ne 200) {
            Send-Json $ctx $State.TenantCurrentStatus @{ status = $State.TenantCurrentStatus; detail = 'No implementado.' }
            continue
          }
          Send-Json $ctx 200 @{ id = $State.Tenant.id; tradeName = $State.Tenant.tradeName; ruc = $State.Tenant.ruc; status = $State.Tenant.status; phone = $State.Tenant.phone; address = $State.Tenant.address }
          continue
        }

        if ($method -eq 'PATCH' -and $path -eq '/api/v1/tenants/current') {
          if ($State.TenantCurrentStatus -ne 200) {
            Send-Json $ctx $State.TenantCurrentStatus @{ status = $State.TenantCurrentStatus; detail = 'No implementado.' }
            continue
          }
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          if ($body.tradeName) { $State.Tenant.tradeName = $body.tradeName }
          $State.Tenant.phone = $body.phone
          $State.Tenant.address = $body.address
          Send-Json $ctx 200 @{ id = $State.Tenant.id; tradeName = $State.Tenant.tradeName; ruc = $State.Tenant.ruc; status = $State.Tenant.status; phone = $State.Tenant.phone; address = $State.Tenant.address }
          continue
        }

        if ($method -eq 'GET' -and $path -eq '/api/v1/users/me') {
          $u = $State.Users | Where-Object { $_.id -eq $caller.UserId } | Select-Object -First 1
          Send-Json $ctx 200 @{
            id = $u.id; fullName = $u.fullName; email = $u.email; role = $u.role; status = $u.status
            tenant = @{ id = $State.Tenant.id; tradeName = $State.Tenant.tradeName; ruc = $State.Tenant.ruc; status = $State.Tenant.status }
          }
          continue
        }

        if ($method -eq 'GET' -and $path -eq '/api/v1/users') {
          $res = @($State.Users | ForEach-Object { @{ id = $_.id; fullName = $_.fullName; email = $_.email; role = $_.role; status = $_.status } })
          Send-Json $ctx 200 $res
          continue
        }

        if ($method -eq 'POST' -and $path -eq '/api/v1/users') {
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          $email = $body.email.ToLowerInvariant()
          if ($State.Users | Where-Object { $_.email -eq $email }) { Send-Json $ctx 409 @{ status = 409; detail = 'Correo duplicado.' }; continue }
          $uid = [guid]::NewGuid().ToString()
          $invTok = 'inv+' + $uid.Substring(0, 8) + '=='
          $u = @{ id = $uid; fullName = $body.fullName; email = $email; role = $body.role; status = 'INVITED'; password = $null; inviteToken = $invTok }
          $null = $State.Users.Add($u)
          $null = $State.Mailbox.Add(@{
            id = [guid]::NewGuid().ToString(); recipient = $email; purpose = 'INVITATION'; state = 'ACTIVE'
            createdAt = [DateTime]::UtcNow.ToString('o'); actionPath = "/accept-invitation?token=$([Uri]::EscapeDataString($invTok))"
          })
          Send-Json $ctx 201 @{ id = $u.id; fullName = $u.fullName; email = $u.email; role = $u.role; status = $u.status }
          continue
        }

        if ($method -eq 'GET' -and $path -eq '/api/v1/vehicles') {
          Send-Json $ctx 200 @($State.Vehicles)
          continue
        }

        if ($method -eq 'POST' -and $path -eq '/api/v1/vehicles') {
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          if ($State.Vehicles | Where-Object { $_.plateNumber -eq $body.plateNumber }) { Send-Json $ctx 409 @{ status = 409; detail = 'Placa duplicada.' }; continue }
          $v = @{ id = [guid]::NewGuid().ToString(); plateNumber = $body.plateNumber; brand = $body.brand; model = $body.model; modelYear = $body.modelYear; payloadCapacityKg = $body.payloadCapacityKg; status = 'AVAILABLE' }
          $null = $State.Vehicles.Add($v)
          Send-Json $ctx 201 $v
          continue
        }

        if ($method -eq 'GET' -and $path -match '^/api/v1/vehicles/([^/]+)/health-status$') {
          $vid = $Matches[1]
          $v = $State.Vehicles | Where-Object { $_.id -eq $vid } | Select-Object -First 1
          $m = $State.Maintenances | Where-Object { $_.vehicleId -eq $vid } | Select-Object -Last 1
          $nextDate = if ($null -ne $m) { $m.nextMaintenanceDate } else { $null }
          $mState = 'UP_TO_DATE'
          if ($null -ne $nextDate) {
            $d = [DateTime]::Parse($nextDate).Date
            $today = [DateTime]::UtcNow.Date
            if ($d -lt $today) { $mState = 'OVERDUE' }
            elseif ($d -le $today.AddDays(7)) { $mState = 'DUE_SOON' }
          }
          Send-Json $ctx 200 @{ vehicleId = $vid; plateNumber = $v.plateNumber; vehicleStatus = $v.status; maintenanceState = $mState; nextMaintenanceDate = $nextDate; availableForTrip = ($mState -ne 'OVERDUE') }
          continue
        }

        if ($method -eq 'POST' -and $path -match '^/api/v1/vehicles/([^/]+)/maintenances$') {
          $vid = $Matches[1]
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          $m = @{ id = [guid]::NewGuid().ToString(); vehicleId = $vid; maintenanceType = $body.maintenanceType; executionDate = $body.executionDate; mileage = $body.mileage; cost = $body.cost; nextMaintenanceDate = $body.nextMaintenanceDate; notes = $body.notes }
          $null = $State.Maintenances.Add($m)
          Send-Json $ctx 201 $m
          continue
        }

        if ($method -eq 'GET' -and $path -eq '/api/v1/drivers') {
          Send-Json $ctx 200 @($State.Drivers)
          continue
        }

        if ($method -eq 'POST' -and $path -eq '/api/v1/drivers') {
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          $u = $State.Users | Where-Object { $_.id -eq $body.userId } | Select-Object -First 1
          if ($null -eq $u -or $u.role -ne 'DRIVER' -or $u.status -ne 'ACTIVE') { Send-Json $ctx 409 @{ status = 409; detail = 'La cuenta del conductor no está activa.' }; continue }
          $d = @{ id = [guid]::NewGuid().ToString(); userId = $body.userId; fullName = $body.fullName; documentType = $body.documentType; documentNumber = $body.documentNumber; licenseNumber = $body.licenseNumber; licenseCategory = $body.licenseCategory; licenseExpirationDate = $body.licenseExpirationDate; status = 'ENABLED' }
          $null = $State.Drivers.Add($d)
          Send-Json $ctx 201 $d
          continue
        }

        if ($method -eq 'GET' -and $path -eq '/api/v1/trips') {
          Send-Json $ctx 200 @($State.Trips)
          continue
        }

        if ($method -eq 'GET' -and $path -match '^/api/v1/trips/([^/]+)$') {
          $tid = $Matches[1]
          $t = $State.Trips | Where-Object { $_.id -eq $tid } | Select-Object -First 1
          if ($null -eq $t) { Send-Json $ctx 404 @{ status = 404; detail = 'El viaje no existe.' }; continue }
          Send-Json $ctx 200 $t
          continue
        }

        if ($method -eq 'POST' -and $path -eq '/api/v1/trips') {
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          $State.TripSeq++
          $code = 'TRP-' + $State.TripSeq.ToString('000000')
          $t = @{ id = [guid]::NewGuid().ToString(); code = $code; origin = $body.origin; destination = $body.destination; cargoDescription = $body.cargoDescription; cargoWeightKg = $body.cargoWeightKg; departureDate = $body.departureDate; status = 'SCHEDULED'; vehicleId = $null; driverId = $null }
          $null = $State.Trips.Add($t)
          Send-Json $ctx 201 $t
          continue
        }

        if ($method -eq 'PUT' -and $path -match '^/api/v1/trips/([^/]+)/assignment$') {
          $tid = $Matches[1]
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          $t = $State.Trips | Where-Object { $_.id -eq $tid } | Select-Object -First 1
          $m = $State.Maintenances | Where-Object { $_.vehicleId -eq $body.vehicleId } | Select-Object -Last 1
          if ($null -ne $m -and ([DateTime]::Parse($m.nextMaintenanceDate).Date -lt [DateTime]::UtcNow.Date) -and (-not $body.confirmOverdueMaintenance)) {
            Send-Json $ctx 409 @{ status = 409; detail = 'Mantenimiento vencido sin confirmación.' }
            continue
          }
          $t.vehicleId = $body.vehicleId
          $t.driverId = $body.driverId
          $t.status = 'ASSIGNED'
          Send-Json $ctx 200 @{ id = $t.id; code = $t.code; status = $t.status; vehicleId = $t.vehicleId; driverId = $t.driverId; assignedAt = [DateTime]::UtcNow.ToString('o'); maintenanceAlert = $null }
          continue
        }

        if ($method -eq 'GET' -and $path -match '^/api/v1/trips/([^/]+)/advances$') {
          $tid = $Matches[1]
          Send-Json $ctx 200 @($State.Advances | Where-Object { $_.tripId -eq $tid })
          continue
        }

        if ($method -eq 'POST' -and $path -match '^/api/v1/trips/([^/]+)/advances$') {
          $tid = $Matches[1]
          $t = $State.Trips | Where-Object { $_.id -eq $tid } | Select-Object -First 1
          if ($t.status -notin @('SCHEDULED', 'ASSIGNED')) { Send-Json $ctx 409 @{ status = 409; detail = 'El viaje ya inició.' }; continue }
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          $adv = @{ id = [guid]::NewGuid().ToString(); tripId = $tid; amount = $body.amount; currency = $body.currency; deliveryMethod = $body.deliveryMethod }
          $null = $State.Advances.Add($adv)
          Send-Json $ctx 201 $adv
          continue
        }

        if ($method -eq 'POST' -and $path -match '^/api/v1/trips/([^/]+)/start$') {
          $tid = $Matches[1]
          $t = $State.Trips | Where-Object { $_.id -eq $tid } | Select-Object -First 1
          $d = $State.Drivers | Where-Object { $_.id -eq $t.driverId } | Select-Object -First 1
          if ($d.userId -ne $caller.UserId) { Send-Json $ctx 404 @{ status = 404; detail = 'El viaje no existe.' }; continue }
          $t.status = 'IN_ROUTE'
          Send-Json $ctx 200 $t
          continue
        }

        if ($method -eq 'POST' -and $path -match '^/api/v1/trips/([^/]+)/finish$') {
          $tid = $Matches[1]
          $t = $State.Trips | Where-Object { $_.id -eq $tid } | Select-Object -First 1
          $d = $State.Drivers | Where-Object { $_.id -eq $t.driverId } | Select-Object -First 1
          if ($d.userId -ne $caller.UserId) { Send-Json $ctx 404 @{ status = 404; detail = 'El viaje no existe.' }; continue }
          $t.status = 'FINISHED'
          if (-not ($State.Settlements | Where-Object { $_.tripId -eq $tid })) {
            $advSum = ($State.Advances | Where-Object { $_.tripId -eq $tid } | Measure-Object -Property amount -Sum).Sum
            $null = $State.Settlements.Add(@{ id = [guid]::NewGuid().ToString(); tripId = $tid; driverId = $t.driverId; status = 'OPEN'; advanceAmount = $advSum })
          }
          Send-Json $ctx 200 $t
          continue
        }

        if ($method -eq 'GET' -and $path -match '^/api/v1/trips/([^/]+)/expenses$') {
          $tid = $Matches[1]
          Send-Json $ctx 200 @($State.Expenses | Where-Object { $_.tripId -eq $tid })
          continue
        }

        if ($method -eq 'POST' -and $path -match '^/api/v1/trips/([^/]+)/expenses$') {
          $tid = $Matches[1]
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          if ($State.Expenses | Where-Object { $_.idempotencyKey -eq $body.idempotencyKey }) {
            Send-Json $ctx 409 @{ status = 409; detail = 'La clave de idempotencia ya fue utilizada.' }; continue
          }
          $e = @{ id = [guid]::NewGuid().ToString(); tripId = $tid; driverId = $caller.UserId; category = $body.category; amount = $body.amount; currency = $body.currency; expenseDate = $body.expenseDate; status = 'PENDING_SUPPORT'; idempotencyKey = $body.idempotencyKey; isLocked = $false; evidence = $null; observationReason = $null }
          $null = $State.Expenses.Add($e)
          Send-Json $ctx 201 $e
          continue
        }

        if ($method -eq 'POST' -and $path -match '^/api/v1/expenses/([^/]+)/evidences$') {
          $eid = $Matches[1]
          $raw = Read-ReqBytes $ctx
          $text = [Text.Encoding]::Latin1.GetString($raw)
          if ($text -notmatch 'name="?file"?' -or $text -notmatch 'PNG') {
            Send-Json $ctx 400 @{ status = 400; detail = 'Formato o campo file inválido.' }; continue
          }
          $e = $State.Expenses | Where-Object { $_.id -eq $eid } | Select-Object -First 1
          $e.evidence = @{ id = [guid]::NewGuid().ToString(); contentType = 'image/png' }
          if ($e.status -eq 'PENDING_SUPPORT') { $e.status = 'REGISTERED' }
          Send-Json $ctx 201 $e
          continue
        }

        if ($method -eq 'PATCH' -and $path -match '^/api/v1/expenses/([^/]+)/status$') {
          $eid = $Matches[1]
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          $e = $State.Expenses | Where-Object { $_.id -eq $eid } | Select-Object -First 1
          $e.status = $body.status
          $e.observationReason = $body.reason
          Send-Json $ctx 200 $e
          continue
        }

        if ($method -eq 'GET' -and $path -eq '/api/v1/incidents') {
          $tripFilter = ''
          if ($query -match 'tripId=([^&]+)') { $tripFilter = $Matches[1] }
          $matched = if ($tripFilter) { @($State.Incidents | Where-Object { $_.tripId -eq $tripFilter }) } else { @($State.Incidents) }
          $res = @($matched | ForEach-Object {
            @{ id = $_.id; tripId = $_.tripId; vehicleId = $_.vehicleId; reportedBy = $_.reportedBy; type = $_.type; description = $_.description; evidenceUrl = $null; evidence = $null; status = $_.status; reportedAt = $_.reportedAt; resolution = $_.resolution; updatedAt = $_.updatedAt }
          })
          Send-Json $ctx 200 $res
          continue
        }

        if ($method -eq 'POST' -and $path -match '^/api/v1/trips/([^/]+)/incidents$') {
          $tid = $Matches[1]
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          $existing = $State.Incidents | Where-Object { $_.idempotencyKey -eq $body.idempotencyKey } | Select-Object -First 1
          if ($null -ne $existing) {
            Send-Json $ctx 200 @{ id = $existing.id; tripId = $existing.tripId; vehicleId = $existing.vehicleId; reportedBy = $existing.reportedBy; type = $existing.type; description = $existing.description; evidenceUrl = $null; evidence = $null; status = $existing.status; reportedAt = $existing.reportedAt; resolution = $existing.resolution; updatedAt = $existing.updatedAt }
            continue
          }
          $t = $State.Trips | Where-Object { $_.id -eq $tid } | Select-Object -First 1
          if ($t.status -ne 'IN_ROUTE') { Send-Json $ctx 409 @{ status = 409; detail = 'Solo se puede reportar cuando el viaje está en ruta.' }; continue }
          $nowIso = [DateTime]::UtcNow.ToString('o')
          $inc = @{ id = [guid]::NewGuid().ToString(); tripId = $tid; vehicleId = $t.vehicleId; reportedBy = $caller.UserId; type = $body.type; description = $body.description; status = 'OPEN'; idempotencyKey = $body.idempotencyKey; reportedAt = $nowIso; resolution = $null; updatedAt = $nowIso }
          $null = $State.Incidents.Add($inc)
          Send-Json $ctx 201 @{ id = $inc.id; tripId = $inc.tripId; vehicleId = $inc.vehicleId; reportedBy = $inc.reportedBy; type = $inc.type; description = $inc.description; evidenceUrl = $null; evidence = $null; status = $inc.status; reportedAt = $inc.reportedAt; resolution = $inc.resolution; updatedAt = $inc.updatedAt }
          continue
        }

        if ($method -eq 'POST' -and $path -match '^/api/v1/incidents/([^/]+)/close$') {
          $iid = $Matches[1]
          $body = ([Text.Encoding]::UTF8.GetString((Read-ReqBytes $ctx))) | ConvertFrom-Json
          $inc = $State.Incidents | Where-Object { $_.id -eq $iid } | Select-Object -First 1
          $inc.status = 'CLOSED'
          $inc.resolution = @{ action = $body.action }
          $inc.updatedAt = [DateTime]::UtcNow.ToString('o')
          Send-Json $ctx 200 @{ id = $inc.id; tripId = $inc.tripId; vehicleId = $inc.vehicleId; reportedBy = $inc.reportedBy; type = $inc.type; description = $inc.description; evidenceUrl = $null; evidence = $null; status = $inc.status; reportedAt = $inc.reportedAt; resolution = $inc.resolution; updatedAt = $inc.updatedAt }
          continue
        }

        if ($method -eq 'GET' -and $path -match '^/api/v1/trips/([^/]+)/settlement$') {
          $tid = $Matches[1]
          $s = $State.Settlements | Where-Object { $_.tripId -eq $tid } | Select-Object -First 1
          if ($null -eq $s) { Send-Json $ctx 404 @{ status = 404; detail = 'No existe liquidación para este viaje.' }; continue }
          Send-Json $ctx 200 $s
          continue
        }

        if ($method -eq 'POST' -and $path -match '^/api/v1/settlements/([^/]+)/close$') {
          $sid = $Matches[1]
          $s = $State.Settlements | Where-Object { $_.id -eq $sid } | Select-Object -First 1
          $pending = @($State.Expenses | Where-Object { $_.tripId -eq $s.tripId -and $_.status -in @('PENDING_SUPPORT', 'REGISTERED') })
          if ($pending.Count -gt 0) { Send-Json $ctx 409 @{ status = 409; detail = 'Hay gastos pendientes.' }; continue }
          $s.status = 'CLOSED'
          $t = $State.Trips | Where-Object { $_.id -eq $s.tripId } | Select-Object -First 1
          $t.status = 'SETTLED'
          foreach ($e in ($State.Expenses | Where-Object { $_.tripId -eq $s.tripId })) { $e.isLocked = $true }
          Send-Json $ctx 200 $s
          continue
        }

        Send-Json $ctx 404 @{ status = 404; detail = "Ruta no simulada: $method $path" }
      } catch {
        Send-Json $ctx 500 @{ status = 500; detail = $_.Exception.Message }
      }
    }
  })
  $async = $ps.BeginInvoke()

  return [pscustomobject]@{
    BaseUrl  = $baseUrl
    State    = $state
    Listener = $http
    Ps       = $ps
    Async    = $async
    Runspace = $runspace
  }
}

function Stop-MockGatewayServer {
  param($Server)
  if ($null -eq $Server) { return }
  $Server.State.StopRequested = $true
  try { $Server.Listener.Stop() } catch {}
  try { $Server.Listener.Close() } catch {}
  try { $Server.Ps.EndInvoke($Server.Async) } catch {}
  try { $Server.Ps.Dispose() } catch {}
  try { $Server.Runspace.Close() } catch {}
  try { $Server.Runspace.Dispose() } catch {}
}

# 1. Prueba de parseo AST de scripts/seed-demo.ps1
$tokens = $null
$parseErrors = $null
$null = [System.Management.Automation.Language.Parser]::ParseFile($scriptPath, [ref]$tokens, [ref]$parseErrors)
Assert-Equal $parseErrors.Count 0 'AST de seed-demo.ps1 sin errores de sintaxis'

# Cargar funciones puras y orquestadores mediante dot-sourcing
. $scriptPath

# 2. Pruebas de utilidades puras
$png = New-DemoPngBytes
Assert-True ($png.Length -ge 8) 'PNG en memoria generado'
Assert-Equal $png[0] 0x89 'Firma PNG byte 0'
Assert-Equal $png[1] 0x50 'Firma PNG byte 1'
Assert-Equal $png[2] 0x4E 'Firma PNG byte 2'
Assert-Equal $png[3] 0x47 'Firma PNG byte 3'

$decoded = Get-TokenFromMailboxActionPath -ActionPath '/verify-email?tenant=abc-123&token=verif%2Btok%2F1%3D%3D'
Assert-Equal $decoded.TenantId 'abc-123' 'Tenant extraído de actionPath'
Assert-Equal $decoded.Token 'verif+tok/1==' 'Token URL-decodificado desde actionPath'

# 3. Validación de parámetros y variables faltantes o inválidas
$tempDir = Join-Path ([IO.Path]::GetTempPath()) ('rinde-seed-test-' + [guid]::NewGuid().ToString('N').Substring(0, 8))
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

$savedEnv = @{}
$envKeys = @(
  'RINDE_GATEWAY_URL', 'DEMO_COMPANY_TRADE_NAME', 'DEMO_COMPANY_RUC', 'DEMO_COMPANY_PHONE',
  'DEMO_COMPANY_ADDRESS', 'DEMO_ADMIN_NAME', 'DEMO_ADMIN_EMAIL', 'DEMO_ADMIN_PASSWORD',
  'DEMO_OPS_NAME', 'DEMO_OPS_EMAIL', 'DEMO_OPS_PASSWORD', 'DEMO_DRIVER1_NAME',
  'DEMO_DRIVER1_EMAIL', 'DEMO_DRIVER1_PASSWORD', 'DEMO_DRIVER2_NAME', 'DEMO_DRIVER2_EMAIL',
  'DEMO_DRIVER2_PASSWORD', 'PLATFORM_OWNER_EMAIL', 'PLATFORM_OWNER_PASSWORD', 'DEMO_VERIFICATION_TOKEN'
)
foreach ($k in $envKeys) {
  $savedEnv[$k] = [Environment]::GetEnvironmentVariable($k)
  [Environment]::SetEnvironmentVariable($k, $null)
}

try {
  Assert-ThrowsContaining {
    Resolve-DemoConfiguration -EnvFileParam (Join-Path $tempDir 'no-existe.env') -RepoRoot $tempDir
  } 'No se encontró el archivo de entorno' 'Rechazo cuando falta EnvFile'

  $missingEnvPath = Join-Path $tempDir 'missing.env'
  Set-Content -LiteralPath $missingEnvPath -Value "DEMO_COMPANY_RUC=20601234567`nDEMO_ADMIN_EMAIL=a@b.invalid" -Encoding utf8
  Assert-ThrowsContaining {
    Resolve-DemoConfiguration -EnvFileParam $missingEnvPath -RepoRoot $tempDir
  } 'DEMO_ADMIN_PASSWORD' 'Rechazo explícito indicando variables faltantes'

  $placeholderEnvPath = Join-Path $tempDir 'placeholder.env'
  Set-Content -LiteralPath $placeholderEnvPath -Value @"
DEMO_COMPANY_RUC=20601234567
DEMO_ADMIN_EMAIL=admin@rutasegura.example.invalid
DEMO_ADMIN_PASSWORD=REPLACE_WITH_DEMO_ADMIN_PASSWORD
DEMO_OPS_EMAIL=ops@rutasegura.example.invalid
DEMO_OPS_PASSWORD=Test-Ops-2026!
DEMO_DRIVER1_EMAIL=d1@rutasegura.example.invalid
DEMO_DRIVER1_PASSWORD=Test-Drv1-2026!
DEMO_DRIVER2_EMAIL=d2@rutasegura.example.invalid
DEMO_DRIVER2_PASSWORD=Test-Drv2-2026!
"@ -Encoding utf8
  Assert-ThrowsContaining {
    Resolve-DemoConfiguration -EnvFileParam $placeholderEnvPath -RepoRoot $tempDir
  } 'valores de plantilla' 'Rechazo de contraseñas plantilla'

  $badRucPath = Join-Path $tempDir 'badruc.env'
  Set-Content -LiteralPath $badRucPath -Value @"
DEMO_COMPANY_RUC=123
DEMO_ADMIN_EMAIL=admin@rutasegura.example.invalid
DEMO_ADMIN_PASSWORD=Test-Admin-2026!
DEMO_OPS_EMAIL=ops@rutasegura.example.invalid
DEMO_OPS_PASSWORD=Test-Ops-2026!
DEMO_DRIVER1_EMAIL=d1@rutasegura.example.invalid
DEMO_DRIVER1_PASSWORD=Test-Drv1-2026!
DEMO_DRIVER2_EMAIL=d2@rutasegura.example.invalid
DEMO_DRIVER2_PASSWORD=Test-Drv2-2026!
"@ -Encoding utf8
  Assert-ThrowsContaining {
    Resolve-DemoConfiguration -EnvFileParam $badRucPath -RepoRoot $tempDir
  } '11 dígitos' 'Rechazo de RUC inválido'

  $validEnvPath = Join-Path $tempDir 'valid.env'
  Set-Content -LiteralPath $validEnvPath -Value @"
RINDE_GATEWAY_URL=http://env.example.invalid:9000/
DEMO_COMPANY_RUC=20601234567
DEMO_ADMIN_EMAIL=admin@rutasegura.example.invalid
DEMO_ADMIN_PASSWORD=Test-Admin-2026!
DEMO_OPS_EMAIL=ops@rutasegura.example.invalid
DEMO_OPS_PASSWORD=Test-Ops-2026!
DEMO_DRIVER1_EMAIL=d1@rutasegura.example.invalid
DEMO_DRIVER1_PASSWORD=Test-Drv1-2026!
DEMO_DRIVER2_EMAIL=d2@rutasegura.example.invalid
DEMO_DRIVER2_PASSWORD=Test-Drv2-2026!
PLATFORM_OWNER_EMAIL=owner@example.invalid
PLATFORM_OWNER_PASSWORD=Test-Owner-2026!
"@ -Encoding utf8

  $cfgOverride = Resolve-DemoConfiguration -GatewayUrlParam 'https://gw.example.invalid/' -EnvFileParam $validEnvPath -RepoRoot $tempDir
  Assert-Equal $cfgOverride.GatewayUrl 'https://gw.example.invalid' 'Parametro GatewayUrl sobrescribe archivo env y quita barra final'

  # 4. Prueba de flujo completo e idempotencia contra HttpListener mock
  $server = Start-MockGatewayServer -MailboxEnabled $true
  try {
    $run1Result = Invoke-DemoSeed -GatewayUrl $server.BaseUrl -EnvFile $validEnvPath
    $run1Text = $run1Result.SummaryLines -join "`n"

    Assert-True ($run1Result.Auth.TenantCreated) 'Empresa creada en corrida 1'
    Assert-True ($run1Result.Auth.TenantVerified) 'Empresa verificada en corrida 1'
    Assert-Equal $run1Result.Auth.UsersCreated 3 'Tres usuarios invitados en corrida 1'
    Assert-Equal $run1Result.Auth.UsersActivated 3 'Tres usuarios activados en corrida 1'
    Assert-Equal $run1Result.Fleet.VehiclesCreated 3 'Tres vehículos creados en corrida 1'
    Assert-Equal $run1Result.Fleet.MaintenancesCreated 3 'Tres mantenimientos creados en corrida 1'
    Assert-Equal $run1Result.Fleet.DriversCreated 2 'Dos conductores creados en corrida 1'
    Assert-Equal $run1Result.Operations.Counters.TripsCreated 5 'Cinco viajes creados en corrida 1'
    Assert-Equal $run1Result.Operations.Counters.AdvancesCreated 4 'Cuatro adelantos creados en corrida 1'
    Assert-Equal $run1Result.Operations.Counters.ExpensesCreated 7 'Siete gastos creados en corrida 1'
    Assert-Equal $run1Result.Operations.Counters.EvidencesUploaded 6 'Seis comprobantes PNG subidos en corrida 1'
    Assert-Equal $run1Result.Operations.Counters.IncidentsCreated 3 'Tres incidencias creadas en corrida 1'
    Assert-Equal $run1Result.Operations.Counters.IncidentsClosed 2 'Dos incidencias cerradas en corrida 1'
    Assert-Equal $run1Result.Operations.Counters.SettlementsClosed 1 'Una liquidación cerrada en corrida 1'

    $tripStatuses = @($server.State.Trips | ForEach-Object { $_.status })
    foreach ($expectedStatus in @('SCHEDULED', 'ASSIGNED', 'IN_ROUTE', 'FINISHED', 'SETTLED')) {
      Assert-True ($tripStatuses -contains $expectedStatus) "Estado de viaje cubierto: $expectedStatus"
    }
    $expStatuses = @($server.State.Expenses | ForEach-Object { $_.status })
    foreach ($expectedExp in @('PENDING_SUPPORT', 'REGISTERED', 'APPROVED', 'OBSERVED')) {
      Assert-True ($expStatuses -contains $expectedExp) "Estado de gasto cubierto: $expectedExp"
    }
    $advMethods = @($server.State.Advances | ForEach-Object { $_.deliveryMethod })
    Assert-True ($advMethods -contains 'CASH' -and $advMethods -contains 'TRANSFER') 'Adelantos CASH y TRANSFER cubiertos'

    Assert-True ($run1Text -notlike '*Test-Admin-2026!*') 'Resumen sin contraseñas'
    Assert-True ($run1Text -notlike '*user:*') 'Resumen sin tokens'

    # Segunda ejecución contra el mismo servidor: debe ser completamente idempotente
    $run2Result = Invoke-DemoSeed -GatewayUrl $server.BaseUrl -EnvFile $validEnvPath

    Assert-True (-not $run2Result.Auth.TenantCreated) 'Empresa reutilizada en corrida 2'
    Assert-Equal $run2Result.Auth.UsersCreated 0 'Cero usuarios duplicados en corrida 2'
    Assert-Equal $run2Result.Fleet.VehiclesCreated 0 'Cero vehículos duplicados en corrida 2'
    Assert-Equal $run2Result.Fleet.MaintenancesCreated 0 'Cero mantenimientos duplicados en corrida 2'
    Assert-Equal $run2Result.Fleet.DriversCreated 0 'Cero conductores duplicados en corrida 2'
    Assert-Equal $run2Result.Operations.Counters.TripsCreated 0 'Cero viajes duplicados en corrida 2'
    Assert-Equal $run2Result.Operations.Counters.AdvancesCreated 0 'Cero adelantos duplicados en corrida 2'
    Assert-Equal $run2Result.Operations.Counters.ExpensesCreated 0 'Cero gastos duplicados en corrida 2'
    Assert-Equal $run2Result.Operations.Counters.EvidencesUploaded 0 'Cero comprobantes duplicados en corrida 2'
    Assert-Equal $run2Result.Operations.Counters.IncidentsCreated 0 'Cero incidencias duplicadas en corrida 2'
    Assert-Equal $run2Result.Operations.Counters.SettlementsClosed 0 'Cero liquidaciones cerradas repetidas en corrida 2'
    Assert-Equal $server.State.Trips.Count 5 'Total viajes permanece en 5'
    Assert-Equal $server.State.Expenses.Count 7 'Total gastos permanece en 7'
    Assert-Equal $server.State.Incidents.Count 3 'Total incidencias permanece en 3'
  } finally {
    Stop-MockGatewayServer -Server $server
  }

  # 5. Compatibilidad cuando /api/v1/tenants/current responde 501 y respaldo por /api/v1/users/me
  $server501 = Start-MockGatewayServer -MailboxEnabled $true -TenantCurrentStatus 501
  try {
    $run501First = Invoke-DemoSeed -GatewayUrl $server501.BaseUrl -EnvFile $validEnvPath
    Assert-True ($run501First.Auth.TenantVerified) 'Empresa verificada con /tenants/current en 501'
    Assert-True (-not [string]::IsNullOrWhiteSpace($run501First.Auth.TenantId)) 'TenantId resuelto con 501 en corrida 1'
    $run501Second = Invoke-DemoSeed -GatewayUrl $server501.BaseUrl -EnvFile $validEnvPath
    Assert-Equal $run501Second.Auth.TenantId $run501First.Auth.TenantId 'TenantId resuelto vía /api/v1/users/me en corrida 2 con 501'
    Assert-Equal $run501Second.Operations.Counters.IncidentsCreated 0 'Cero incidencias duplicadas en corrida 2 con 501'
  } finally {
    Stop-MockGatewayServer -Server $server501
  }

  # 6. Buzón deshabilitado devuelve instrucción clara
  $serverNoMailbox = Start-MockGatewayServer -MailboxEnabled $false
  try {
    Assert-ThrowsContaining {
      $null = Invoke-DemoSeed -GatewayUrl $serverNoMailbox.BaseUrl -EnvFile $validEnvPath
    } 'buzón de demostración' 'Mensaje claro cuando el buzón no está disponible'
  } finally {
    Stop-MockGatewayServer -Server $serverNoMailbox
  }

  Write-Output 'Todas las pruebas de seed-demo.ps1 pasaron correctamente (AST, validación, flujo completo, idempotencia y buzón deshabilitado).'
} finally {
  foreach ($k in $envKeys) {
    [Environment]::SetEnvironmentVariable($k, $savedEnv[$k])
  }
  Remove-Item -LiteralPath $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
