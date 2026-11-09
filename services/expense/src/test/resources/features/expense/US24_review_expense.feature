# language: es
Característica: US24 Revisar y aprobar los gastos de un viaje
  Como responsable de operaciones
  Quiero revisar cada gasto registrado junto con su evidencia y aprobarlo u observarlo
  Para validar la rendición sin recopilar comprobantes por separado

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Y que existe el vehículo "camión"
    Y que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"
    Y que el conductor "luis@andes.pe" inició sesión
    Y que el conductor inicia el viaje "viaje"
    Y que el conductor registró un gasto "combustible" de tipo "FUEL" por el monto "200.00" con evidencia "factura.jpg" y clave "KEY-US24-01" en el viaje "viaje"
    Y que el administrador "ana@andes.pe" inició sesión

  Escenario: Aprobar un gasto registrado
    Cuando el administrador aprueba el gasto "combustible"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "status" con el valor "APPROVED"

  Escenario: Observar un gasto por inconsistencia y publicar evento
    Cuando el administrador observa el gasto "combustible" con el motivo "Comprobante térmico borroso"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "status" con el valor "OBSERVED"
    Y la respuesta incluye el campo "observationReason" con el valor "Comprobante térmico borroso"
    Y se publica el evento "ExpenseObserved" para "luis@andes.pe"

  Escenario: Rechazar observación sin motivo explicativo
    Cuando el administrador intenta observar el gasto "combustible" sin motivo
    Entonces la respuesta tiene código 400

  Escenario: Un conductor no puede aprobar u observar gastos
    Dado que el conductor "luis@andes.pe" inició sesión
    Cuando el conductor "luis@andes.pe" intenta aprobar el gasto "combustible"
    Entonces la respuesta tiene código 403

  Escenario: No se puede modificar un gasto de un viaje cuya liquidación fue cerrada
    Dado que el conductor "luis@andes.pe" finaliza el viaje "viaje"
    Y que la liquidación del viaje "viaje" de la empresa con RUC "20123456789" fue cerrada
    Cuando el administrador aprueba el gasto "combustible"
    Entonces la respuesta tiene código 409
    Y el mensaje de error es "El gasto se encuentra bloqueado por liquidación cerrada."
