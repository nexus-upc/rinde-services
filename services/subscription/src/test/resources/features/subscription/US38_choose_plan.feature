# language: es
Característica: US38 Elegir un plan de suscripción
  Como administrador de una empresa
  Quiero elegir un plan según la cantidad de unidades de mi flota
  Para pagar solo por la capacidad que necesito

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión

  Escenario: Consultar y seleccionar un plan disponible
    Dado que existe el plan activo de prueba "Plan-Prueba" con precio mensual "37.50" y límite 5
    Cuando el administrador consulta los planes disponibles
    Entonces la respuesta tiene código 200
    Y el catálogo incluye el plan "Plan-Prueba" con precio "37.50" y límite 5
    Cuando el administrador selecciona el plan "Plan-Prueba"
    Entonces la respuesta tiene código 201
    Y la suscripción creada tiene estado "PENDING_PAYMENT"
    Y la suscripción creada no tiene vigencia pagada
    Cuando el administrador consulta la suscripción creada
    Entonces la respuesta tiene código 200
    Y la suscripción consultada tiene el estado "PENDING_PAYMENT"

  Escenario: Rechazar un plan que no cubre la flota e informar el exceso
    Dado que existe el plan activo de prueba "Plan-Limitado" con precio mensual "19.00" y límite 2
    Y que la empresa tiene 3 vehículos registrados
    Cuando el administrador intenta seleccionar el plan "Plan-Limitado"
    Entonces la respuesta tiene código 409
    Y el mensaje de error contiene "1 unidad"
    Y no se crea una suscripción para la empresa

  Escenario: Un conductor no puede crear una suscripción
    Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Y que existe el plan activo de prueba "Plan-Restringido" con precio mensual "25.00" y límite 4
    Cuando el conductor "luis@andes.pe" intenta seleccionar el plan "Plan-Restringido"
    Entonces la respuesta tiene código 403

  Escenario: Consultar el catálogo sin token requiere autenticación
    Cuando una persona sin token consulta los planes disponibles
    Entonces la respuesta tiene código 401
