# Prueba el administrador de RabbitMQ de punta a punta y muestra un resumen.
# Uso (en PowerShell, desde la raiz del repo):
#   $t = "pega-aqui-el-token-de-auditoria"
#   powershell -ExecutionPolicy Bypass -File .\rabbit-admin-service\probar-admin.ps1 -Token $t
# Opcional: -TokenCliente para comprobar que un usuario sin rol AUDITORIA recibe 403.

param(
    [Parameter(Mandatory = $true)][string]$Token,
    [string]$TokenCliente = "",
    [string]$Base = "http://localhost:8083"
)

function Invocar($Metodo, $Ruta, $Cuerpo, $TokenAUsar) {
    $cabeceras = @{}
    if ($TokenAUsar) { $cabeceras["Authorization"] = "Bearer $TokenAUsar" }
    try {
        if ($null -ne $Cuerpo) {
            $r = Invoke-WebRequest -UseBasicParsing -Method $Metodo -Uri ($Base + $Ruta) -Headers $cabeceras -ContentType "application/json" -Body $Cuerpo
        } else {
            $r = Invoke-WebRequest -UseBasicParsing -Method $Metodo -Uri ($Base + $Ruta) -Headers $cabeceras
        }
        return [int]$r.StatusCode
    } catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        return -1
    }
}

$pasos = @(
    @{ Nombre = "Sin token es rechazado";                  Esperado = 401; Metodo = "GET";    Ruta = "/api/queues/cocina.comanda.q"; Cuerpo = $null; Token = "" },
    @{ Nombre = "Consultar una cola existente";            Esperado = 200; Metodo = "GET";    Ruta = "/api/queues/cocina.comanda.q"; Cuerpo = $null; Token = $Token },
    @{ Nombre = "Consultar una cola que no existe";        Esperado = 404; Metodo = "GET";    Ruta = "/api/queues/no.existe.q"; Cuerpo = $null; Token = $Token },
    @{ Nombre = "Crear cola con nombre vacio";             Esperado = 400; Metodo = "POST";   Ruta = "/api/queues"; Cuerpo = '{"nombre":""}'; Token = $Token },
    @{ Nombre = "Crear cola con nombre invalido";          Esperado = 400; Metodo = "POST";   Ruta = "/api/queues"; Cuerpo = '{"nombre":"nombre con espacios"}'; Token = $Token },
    @{ Nombre = "Crear cola demo.admin.q";                 Esperado = 201; Metodo = "POST";   Ruta = "/api/queues"; Cuerpo = '{"nombre":"demo.admin.q","deadLetterExchange":"pedidos.dlx","deadLetterRoutingKey":"demo.admin.dlq"}'; Token = $Token },
    @{ Nombre = "Crear exchange demo.admin.ex (topic)";    Esperado = 201; Metodo = "POST";   Ruta = "/api/exchanges"; Cuerpo = '{"nombre":"demo.admin.ex","tipo":"TOPIC"}'; Token = $Token },
    @{ Nombre = "Crear exchange con tipo inexistente";     Esperado = 400; Metodo = "POST";   Ruta = "/api/exchanges"; Cuerpo = '{"nombre":"otro.ex","tipo":"inventado"}'; Token = $Token },
    @{ Nombre = "Crear binding demo.admin.ex -> cola";     Esperado = 201; Metodo = "POST";   Ruta = "/api/bindings"; Cuerpo = '{"exchange":"demo.admin.ex","cola":"demo.admin.q","routingKey":"demo.#"}'; Token = $Token },
    @{ Nombre = "Crear binding a una cola inexistente";    Esperado = 404; Metodo = "POST";   Ruta = "/api/bindings"; Cuerpo = '{"exchange":"demo.admin.ex","cola":"no.existe.q","routingKey":"demo.#"}'; Token = $Token },
    @{ Nombre = "Eliminar el binding creado";              Esperado = 204; Metodo = "DELETE"; Ruta = "/api/bindings?exchange=demo.admin.ex&cola=demo.admin.q&routingKey=demo.%23"; Cuerpo = $null; Token = $Token },
    @{ Nombre = "Eliminar el exchange demo.admin.ex";      Esperado = 204; Metodo = "DELETE"; Ruta = "/api/exchanges/demo.admin.ex"; Cuerpo = $null; Token = $Token },
    @{ Nombre = "Eliminar la cola demo.admin.q";           Esperado = 204; Metodo = "DELETE"; Ruta = "/api/queues/demo.admin.q"; Cuerpo = $null; Token = $Token },
    @{ Nombre = "Eliminar una cola del sistema (protegida)";    Esperado = 409; Metodo = "DELETE"; Ruta = "/api/queues/cocina.comanda.q"; Cuerpo = $null; Token = $Token },
    @{ Nombre = "Eliminar un exchange del sistema (protegido)"; Esperado = 409; Metodo = "DELETE"; Ruta = "/api/exchanges/pedidos.exchange"; Cuerpo = $null; Token = $Token }
)

if ($TokenCliente) {
    $pasos += @{ Nombre = "Un usuario sin rol AUDITORIA es rechazado"; Esperado = 403; Metodo = "GET"; Ruta = "/api/queues/cocina.comanda.q"; Cuerpo = $null; Token = $TokenCliente }
}

$fallos = 0
foreach ($paso in $pasos) {
    $obtenido = Invocar $paso.Metodo $paso.Ruta $paso.Cuerpo $paso.Token
    if ($obtenido -eq $paso.Esperado) { $marca = "[OK]    " } else { $marca = "[FALLO] "; $fallos++ }
    Write-Host ("{0}{1}  (esperado {2}, obtenido {3})" -f $marca, $paso.Nombre, $paso.Esperado, $obtenido)
}

Write-Host ""
if ($fallos -eq 0) { Write-Host "Todo correcto." } else { Write-Host "$fallos paso(s) con resultado distinto al esperado." }