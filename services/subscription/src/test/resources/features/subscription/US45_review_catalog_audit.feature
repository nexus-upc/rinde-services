# language: es
Característica: US45 Revisar la auditoría del catálogo de la plataforma
  Como dueño de Rinde
  Quiero revisar los cambios del catálogo con su actor, fecha y motivo
  Para conservar un historial que no pueda editarse ni eliminarse desde la API

  Antecedentes:
    Dado que el dueño de Rinde inició sesión en el panel de la plataforma
    Y que el dueño registró el plan "Plan Auditoría" con precio mensual "90.00", límite 5 y motivo "Alta para auditoría"

  Escenario: Historial del alta, retiro y nueva publicación de un plan
    Cuando el dueño retira el plan "Plan Auditoría" con motivo "Retiro por revisión"
    Y el dueño vuelve a publicar el plan "Plan Auditoría" con motivo "Reactivación"
    Y el dueño consulta la auditoría del catálogo
    Entonces la auditoría del catálogo conserva actor, fecha, motivos y estados

  Escenario: La API no permite editar ni eliminar el historial del catálogo
    Cuando el dueño intenta editar y eliminar la entrada de auditoría del catálogo
    Entonces la respuesta tiene código 200
    Y la auditoría del catálogo registra 1 cambios

  Escenario: La auditoría no está disponible para las empresas
    Cuando un administrador de empresa consulta la auditoría del catálogo
    Entonces la respuesta no es exitosa
