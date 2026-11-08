# language: es
Característica: US42 Gestionar el acceso de las empresas
  Como dueño de Rinde
  Quiero buscar empresas y cambiar su acceso con un motivo
  Para gestionar incidentes sin modificar su suscripción comercial

  Antecedentes:
    Dado que existe un dueño de plataforma para la prueba
    Y que existe una empresa activa para la prueba de plataforma
    Y que el dueño tiene una sesión de plataforma

  Escenario: Búsqueda paginada por RUC
    Cuando el dueño busca la empresa por su RUC
    Entonces la respuesta tiene código 200
    Y la búsqueda devuelve la empresa con su estado comercial y administrativo

  Escenario: Suspensión de una sesión de empresa que ya estaba abierta
    Cuando el dueño suspende la empresa con motivo "Revisión de soporte"
    Entonces la respuesta tiene código 200
    Y la sesión existente de la empresa queda bloqueada

  Escenario: El pago no levanta una suspensión administrativa
    Dado que el dueño suspendió la empresa para revisión
    Cuando se recibe una activación comercial de la suscripción
    Entonces la suscripción queda activa y la empresa sigue suspendida
    Y la sesión existente de la empresa queda bloqueada

  Escenario: Restablecer el acceso no elimina la restricción comercial
    Dado que el dueño suspendió la empresa para revisión
    Y que la suscripción de la empresa está restringida
    Cuando el dueño restablece la empresa con motivo "Revisión finalizada"
    Entonces la respuesta tiene código 200
    Y la empresa conserva su restricción comercial

  Escenario: El motivo es obligatorio
    Cuando el dueño suspende la empresa con motivo ""
    Entonces la respuesta tiene código 400
    Y el acceso de la empresa permanece habilitado
