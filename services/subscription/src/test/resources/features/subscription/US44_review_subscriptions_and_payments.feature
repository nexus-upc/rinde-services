# language: es
Característica: US44 Consultar las suscripciones y los pagos de las empresas
  Como dueño de Rinde
  Quiero consultar las suscripciones y los intentos de pago de todas las empresas
  Para conocer el estado comercial de la plataforma y atender consultas de los clientes

  Antecedentes:
    Dado que el dueño de Rinde inició sesión en el panel de la plataforma
    Y que la empresa "Transportes Andes" tiene una suscripción activa del plan "Plan Flota" con un pago confirmado de "120.00"
    Y que la empresa "Carga Sur" tiene una suscripción pendiente de pago del plan "Plan Flota"

  Escenario: Resumen comercial de la plataforma
    Cuando el dueño abre el resumen comercial
    Entonces la respuesta tiene código 200
    Y el resumen muestra 2 suscripciones, 1 pendiente de pago y "120.00" en pagos confirmados

  Escenario: Detalle de suscripciones y pagos de una empresa
    Cuando el dueño consulta las suscripciones de la empresa "Transportes Andes"
    Entonces la respuesta tiene código 200
    Y cada registro muestra la empresa "Transportes Andes", el plan "Plan Flota" y su estado
    Cuando el dueño consulta los pagos de la empresa "Transportes Andes"
    Entonces la respuesta tiene código 200
    Y el pago muestra la empresa "Transportes Andes", el importe "120.00" y su referencia

  Escenario: Los pagos simulados se identifican como pagos de prueba
    Cuando el dueño abre el resumen comercial
    Entonces el resumen indica que el modo de pago es "SIMULATED"

  Escenario: Una empresa no puede consultar el panel comercial
    Cuando el administrador de una empresa intenta abrir el resumen comercial
    Entonces la respuesta no es exitosa
