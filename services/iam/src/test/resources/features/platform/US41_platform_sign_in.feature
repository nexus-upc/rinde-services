# language: es
Característica: US41 Ingresar al panel de administración de la plataforma
  Como dueño de Rinde
  Quiero ingresar con una cuenta independiente de las empresas
  Para administrar la plataforma con una sesión propia

  Antecedentes:
    Dado que existe un dueño de plataforma para la prueba

  Escenario: Ingreso con identidad de plataforma
    Cuando el dueño inicia sesión en el acceso de plataforma
    Entonces la respuesta tiene código 200
    Y la sesión permite consultar el perfil de dueño sin crear una empresa

  Escenario: Contraseña de plataforma incorrecta
    Cuando el dueño intenta ingresar con una contraseña incorrecta
    Entonces la respuesta tiene código 401

  Escenario: Las sesiones de empresa y plataforma no son intercambiables
    Dado que existe una empresa activa para la prueba de plataforma
    Y que el dueño tiene una sesión de plataforma
    Cuando se intenta intercambiar las sesiones de empresa y plataforma
    Entonces ambas solicitudes de acceso se rechazan

  Escenario: Un administrador no puede invitar a otro dueño de plataforma
    Dado que existe una empresa activa para la prueba de plataforma
    Cuando el administrador intenta invitar al rol de plataforma
    Entonces la respuesta tiene código 400
