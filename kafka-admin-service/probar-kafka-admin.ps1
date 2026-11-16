# Prueba el administrador de Kafka de punta a punta y muestra un resumen.
# Uso (en PowerShell, desde la raiz del repo):
#   $t = "pega-aqui-el-token-de-auditoria"
#   powershell -ExecutionPolicy Bypass -File .\kafka-admin-service\probar-kafka-admin.ps1 -Token $t
# Opcional: -TokenCliente para comprobar que un usuario sin rol AUDITORIA recibe 403.
#
# Contra el servicio directo (por defecto):  -Base http://localhost:8084
# A traves del API Gateway (rutas /v1/...):  -Base https://pqj1b6z9g5.execute-api.us-east-1.amazonaws.com/dev -Prefijo /v1

param(
    [Parameter(Mandatory = $true)][string]$Token,
    [string]$TokenCliente = "",
    [string]$Base = "http://localhost:8084",
    [string]$Prefijo = "/api"
)

function Invocar($Metodo, $Ruta, $Cuerpo, $TokenAUsar) {
    # Las rutas de abajo se escriben con /api; segun el destino se cambian por el prefijo indicado.
    $Ruta = $Ruta.Replace("/api/", $Prefijo + "/")
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
    @{ Nombre = "Sin token es rechazado";                          Esperado = 401; Metodo = "GET";    Ruta = "/api/topics"; Cuerpo = $null; Token = "" },
    @{ Nombre = "Listar los topicos";                              Esperado = 200; Metodo = "GET";    Ruta = "/api/topics"; Cuerpo = $null; Token = $Token },
    @{ Nombre = "Describir un topico del sistema";                 Esperado = 200; Metodo = "GET";    Ruta = "/api/topics/pedidos.eventos"; Cuerpo = $null; Token = $Token },
    @{ Nombre = "Describir un topico que no existe";               Esperado = 404; Metodo = "GET";    Ruta = "/api/topics/no.existe"; Cuerpo = $null; Token = $Token },
    @{ Nombre = "Crear topico con nombre invalido";                Esperado = 400; Metodo = "POST";   Ruta = "/api/topics"; Cuerpo = '{"nombre":"mi topico","particiones":3,"replicacion":2}'; Token = $Token },
    @{ Nombre = "Crear topico con replicacion imposible";          Esperado = 400; Metodo = "POST";   Ruta = "/api/topics"; Cuerpo = '{"nombre":"demo.admin","particiones":3,"replicacion":3}'; Token = $Token },
    @{ Nombre = "Crear topico demo.admin";                         Esperado = 201; Metodo = "POST";   Ruta = "/api/topics"; Cuerpo = '{"nombre":"demo.admin","particiones":3,"replicacion":2,"configuracion":{"cleanup.policy":"delete","retention.ms":"3600000"}}'; Token = $Token },
    @{ Nombre = "Crear el mismo topico otra vez";                  Esperado = 409; Metodo = "POST";   Ruta = "/api/topics"; Cuerpo = '{"nombre":"demo.admin","particiones":3,"replicacion":2}'; Token = $Token },
    @{ Nombre = "Modificar la retencion";                          Esperado = 200; Metodo = "PATCH";  Ruta = "/api/topics/demo.admin/config"; Cuerpo = '{"configuracion":{"retention.ms":"7200000"}}'; Token = $Token },
    @{ Nombre = "Modificar una configuracion no permitida";        Esperado = 400; Metodo = "PATCH";  Ruta = "/api/topics/demo.admin/config"; Cuerpo = '{"configuracion":{"compression.type":"gzip"}}'; Token = $Token },
    @{ Nombre = "Aumentar las particiones a 4";                    Esperado = 200; Metodo = "PATCH";  Ruta = "/api/topics/demo.admin/partitions"; Cuerpo = '{"total":4}'; Token = $Token },
    @{ Nombre = "Reducir las particiones (Kafka no lo permite)";   Esperado = 400; Metodo = "PATCH";  Ruta = "/api/topics/demo.admin/partitions"; Cuerpo = '{"total":2}'; Token = $Token },
    @{ Nombre = "Listar los consumer groups";                      Esperado = 200; Metodo = "GET";    Ruta = "/api/consumer-groups"; Cuerpo = $null; Token = $Token },
    @{ Nombre = "Eliminar el topico demo.admin";                   Esperado = 204; Metodo = "DELETE"; Ruta = "/api/topics/demo.admin"; Cuerpo = $null; Token = $Token },
    @{ Nombre = "Eliminar un topico del sistema (protegido)";      Esperado = 409; Metodo = "DELETE"; Ruta = "/api/topics/pedidos.eventos"; Cuerpo = $null; Token = $Token }
)

if ($TokenCliente) {
    $pasos += @{ Nombre = "Un usuario sin rol AUDITORIA es rechazado"; Esperado = 403; Metodo = "GET"; Ruta = "/api/topics"; Cuerpo = $null; Token = $TokenCliente }
}

$fallos = 0
foreach ($paso in $pasos) {
    $obtenido = Invocar $paso.Metodo $paso.Ruta $paso.Cuerpo $paso.Token
    if ($obtenido -eq $paso.Esperado) { $marca = "[OK]    " } else { $marca = "[FALLO] "; $fallos++ }
    Write-Host ("{0}{1}  (esperado {2}, obtenido {3})" -f $marca, $paso.Nombre, $paso.Esperado, $obtenido)
}

Write-Host ""
if ($fallos -eq 0) { Write-Host "Todo correcto." } else { Write-Host "$fallos paso(s) con resultado distinto al esperado." }
