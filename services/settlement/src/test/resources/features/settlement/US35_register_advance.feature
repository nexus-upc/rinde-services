# language: es
Característica: US35 Registrar el adelanto de viáticos de un viaje
  Como responsable de operaciones
  Quiero registrar el monto entregado al conductor como adelanto
  Para que la liquidación calcule correctamente el saldo al cierre del viaje

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Y que existe el vehículo "camión"
    Y que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"

  Escenario: Registrar el adelanto de un viaje asignado
    Cuando el administrador registra un adelanto de "500.00" soles por "CASH" para el viaje "viaje"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "amount" con el valor "500.0"
    Y la respuesta incluye el campo "deliveryMethod" con el valor "CASH"
    Y la respuesta incluye el campo "tripId" con el id del viaje "viaje"

  Escenario: Registrar el adelanto de un viaje programado
    Dado que el administrador programa el viaje "otro" de "Cusco" a "Puno" con carga "Cemento" y salida "HOY+9"
    Cuando el administrador registra un adelanto de "150.00" soles por "TRANSFER" para el viaje "otro"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "deliveryMethod" con el valor "TRANSFER"

  Escenario: Rechazar el adelanto de un viaje ya iniciado
    Dado que el conductor inicia el viaje "viaje"
    Cuando el administrador registra un adelanto de "100.00" soles por "CASH" para el viaje "viaje"
    Entonces la respuesta tiene código 409

  Escenario: Rechazar el adelanto con monto cero
    Cuando el administrador registra un adelanto de "0" soles por "CASH" para el viaje "viaje"
    Entonces la respuesta tiene código 400

  Escenario: Rechazar un medio de entrega desconocido
    Cuando el administrador registra un adelanto de "100.00" soles por "BITCOIN" para el viaje "viaje"
    Entonces la respuesta tiene código 400

  Escenario: Rechazar el adelanto de un viaje inexistente
    Cuando el administrador registra un adelanto de "100.00" soles por "CASH" para un viaje inexistente
    Entonces la respuesta tiene código 404

  Escenario: El conductor no puede registrar adelantos
    Cuando el conductor "luis@andes.pe" registra un adelanto de "200.00" soles por "CASH" para el viaje "viaje"
    Entonces la respuesta tiene código 403

  Escenario: Registrar un adelanto requiere autenticación
    Cuando una persona sin token registra un adelanto de "100.00" soles por "CASH" para el viaje "viaje"
    Entonces la respuesta tiene código 401

  Escenario: El responsable y el conductor consultan los adelantos del viaje
    Cuando el administrador registra un adelanto de "300.00" soles por "CASH" para el viaje "viaje"
    Y el administrador consulta los adelantos del viaje "viaje"
    Entonces la respuesta tiene código 200
    Y la respuesta contiene 1 elementos
    Cuando el conductor "luis@andes.pe" consulta los adelantos del viaje "viaje"
    Entonces la respuesta tiene código 200
    Y la respuesta contiene 1 elementos

  Escenario: Los adelantos registrados se suman en la liquidación al finalizar el viaje
    Cuando el administrador registra un adelanto de "300.00" soles por "CASH" para el viaje "viaje"
    Y el administrador registra un adelanto de "200.00" soles por "TRANSFER" para el viaje "viaje"
    Y que el conductor "luis@andes.pe" inició sesión
    Y que el conductor inicia el viaje "viaje"
    Y que el conductor "luis@andes.pe" finaliza el viaje "viaje"
    Cuando el administrador consulta la liquidación del viaje "viaje"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "advanceAmount" con el valor "500.0"
