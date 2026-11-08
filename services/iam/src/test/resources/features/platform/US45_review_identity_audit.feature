# language: es
Característica: US45 Revisar la auditoría de identidad de la plataforma
  Como dueño de Rinde
  Quiero consultar quién cambió el acceso de las empresas
  Para revisar los motivos y los estados anterior y posterior

  Antecedentes:
    Dado que existe un dueño de plataforma para la prueba
    Y que existe una empresa activa para la prueba de plataforma
    Y que el dueño tiene una sesión de plataforma
    Y que el dueño suspendió la empresa para revisión

  Escenario: Historial con actor, fecha, empresa, motivo y estados
    Cuando el dueño consulta la auditoría de identidad
    Entonces la respuesta tiene código 200
    Y la auditoría conserva los datos de la suspensión

  Escenario: La API no permite editar ni eliminar una entrada
    Cuando el dueño intenta editar y eliminar la entrada de auditoría
    Entonces las operaciones no están disponibles y la entrada se conserva

  Escenario: Una sesión de empresa no puede consultar la auditoría
    Cuando un administrador de otra empresa consulta la auditoría de identidad
    Entonces la respuesta tiene código 401
