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

  Escenario: Un administrador de otra empresa no puede aprobar un gasto ajeno
    Dado que existe otra empresa verificada con RUC "20987654321" y administrador "otro@otra.pe"
    Y que el administrador de la otra empresa "otro@otra.pe" inició sesión
    Cuando el administrador "otro@otra.pe" intenta aprobar el gasto "combustible"
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "El gasto no existe."

  Escenario: Consultar en la bandeja global los gastos pendientes de revisión con su comprobante
    Dado que el conductor "luis@andes.pe" registró el gasto "peaje-sin-foto" de tipo "TOLL" por el monto "35.00" en fecha "HOY+6" sin evidencia y clave "KEY-US24-02" en el viaje "viaje"
    Y que el conductor "luis@andes.pe" registró el gasto "almuerzo-aprobado" de tipo "FOOD" por el monto "45.00" en fecha "HOY+4" sin evidencia y clave "KEY-US24-03" en el viaje "viaje"
    Y que el administrador "ana@andes.pe" aprobó el gasto "almuerzo-aprobado"
    Cuando el usuario "ana@andes.pe" consulta la bandeja de gastos con estado "REGISTERED,PENDING_SUPPORT"
    Entonces la respuesta tiene código 200
    Y la página de gastos contiene 2 elementos de un total de 2 en 1 páginas
    Y el elemento 0 de la bandeja corresponde al gasto "peaje-sin-foto"
    Y el elemento 1 de la bandeja corresponde al gasto "combustible"
    Y el elemento 1 de la bandeja tiene el comprobante "factura.jpg"
    Y la respuesta no incluye el gasto "almuerzo-aprobado"

  Escenario: Filtrar la bandeja global por viaje, conductor y periodo con orden descendente
    Dado que el administrador invitó a "Rosa Pérez" con el correo "rosa@andes.pe" y el rol "DRIVER"
    Y que el administrador invitó a "Carlos Ruiz" con el correo "ops@andes.pe" y el rol "OPERATIONS_MANAGER"
    Y que el administrador programa el viaje "viaje-norte" de "Lima" a "Trujillo" con carga "Alimentos" y salida "HOY+2"
    Y que el administrador asigna el viaje "viaje-norte" al vehículo "camión" y al conductor "rosa@andes.pe"
    Y que el conductor "rosa@andes.pe" inicia el viaje "viaje-norte"
    Y que el conductor "rosa@andes.pe" registró el gasto "gasto-rosa" de tipo "FUEL" por el monto "120.00" en fecha "HOY+5" sin evidencia y clave "KEY-US24-04" en el viaje "viaje-norte"
    Y que el conductor "luis@andes.pe" registró el gasto "gasto-reciente" de tipo "TOLL" por el monto "28.00" en fecha "HOY+6" sin evidencia y clave "KEY-US24-05" en el viaje "viaje"
    Y que el conductor "luis@andes.pe" registró el gasto "gasto-fuera-rango" de tipo "FOOD" por el monto "18.00" en fecha "HOY+1" sin evidencia y clave "KEY-US24-06" en el viaje "viaje"
    Cuando el usuario "ops@andes.pe" consulta la bandeja de gastos del viaje "viaje" del conductor "luis@andes.pe" desde "HOY+4" hasta "HOY+6"
    Entonces la respuesta tiene código 200
    Y la página de gastos contiene 2 elementos de un total de 2 en 1 páginas
    Y el elemento 0 de la bandeja corresponde al gasto "gasto-reciente"
    Y el elemento 1 de la bandeja corresponde al gasto "combustible"
    Y la respuesta no incluye el gasto "gasto-rosa"
    Y la respuesta no incluye el gasto "gasto-fuera-rango"

  Escenario: Paginar la bandeja global de gastos en múltiples páginas
    Dado que el conductor "luis@andes.pe" registró el gasto "gasto-dia-6" de tipo "TOLL" por el monto "30.00" en fecha "HOY+6" sin evidencia y clave "KEY-US24-07" en el viaje "viaje"
    Y que el conductor "luis@andes.pe" registró el gasto "gasto-dia-7" de tipo "FOOD" por el monto "50.00" en fecha "HOY+7" sin evidencia y clave "KEY-US24-08" en el viaje "viaje"
    Cuando el usuario "ana@andes.pe" consulta la bandeja de gastos en la página 0 con tamaño 2
    Entonces la respuesta tiene código 200
    Y la página de gastos contiene 2 elementos de un total de 3 en 2 páginas
    Y el elemento 0 de la bandeja corresponde al gasto "gasto-dia-7"
    Y el elemento 1 de la bandeja corresponde al gasto "gasto-dia-6"
    Cuando el usuario "ana@andes.pe" consulta la bandeja de gastos en la página 1 con tamaño 2
    Entonces la respuesta tiene código 200
    Y la página de gastos contiene 1 elementos de un total de 3 en 2 páginas
    Y el elemento 0 de la bandeja corresponde al gasto "combustible"

  Escenario: Un conductor no puede consultar la bandeja global de gastos
    Cuando el usuario "luis@andes.pe" consulta la bandeja de gastos con estado "REGISTERED"
    Entonces la respuesta tiene código 403

  Escenario: Rechazar periodo con fecha final anterior a la inicial en la bandeja de gastos
    Cuando el usuario "ana@andes.pe" consulta la bandeja de gastos desde "HOY+6" hasta "HOY+2"
    Entonces la respuesta tiene código 400
    Y el mensaje de error es "La fecha final no puede ser anterior a la inicial."

  Esquema del escenario: Rechazar parámetros inválidos en la bandeja global de gastos
    Cuando el usuario "ana@andes.pe" consulta la bandeja de gastos con el parámetro "<parametro>" en "<valor>"
    Entonces la respuesta tiene código 400
    Y el mensaje de error es "<mensaje>"

    Ejemplos:
      | parametro | valor                | mensaje                                               |
      | page      | -1                   | El número de página no puede ser negativo.            |
      | size      | 0                    | El tamaño de página debe estar entre 1 y 100.         |
      | size      | 101                  | El tamaño de página debe estar entre 1 y 100.         |
      | from      | 20-11-2026           | La fecha de inicio debe tener el formato AAAA-MM-DD.  |
      | to        | fecha-invalida       | La fecha de fin debe tener el formato AAAA-MM-DD.     |
      | status    | REGISTERED,INVALIDO  | El parámetro status contiene un estado de gasto inválido. |
