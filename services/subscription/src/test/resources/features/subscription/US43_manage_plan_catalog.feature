# language: es
Característica: US43 Gestionar el catálogo de planes
  Como dueño de Rinde
  Quiero crear planes y retirarlos o volver a publicarlos
  Para ajustar la oferta comercial sin alterar las condiciones de las empresas que ya contrataron

  Antecedentes:
    Dado que el dueño de Rinde inició sesión en el panel de la plataforma

  Escenario: Alta de un plan con motivo
    Cuando el dueño registra el plan "Plan Flota" con precio mensual "120.00", límite 15 y motivo "Lanzamiento del catálogo"
    Entonces la respuesta tiene código 201
    Y el plan "Plan Flota" queda publicado con precio "120.00" y límite 15
    Y las empresas ven el plan "Plan Flota" en el catálogo

  Escenario: Un plan existente no admite cambio de precio
    Dado que el dueño registró el plan "Plan Base" con precio mensual "80.00", límite 5 y motivo "Plan inicial"
    Y que una empresa tiene una suscripción pendiente del plan "Plan Base"
    Cuando el dueño intenta cambiar el precio del plan "Plan Base" a "95.00"
    Entonces la respuesta no permite modificar el plan
    Cuando el dueño registra el plan "Plan Base 2" con precio mensual "95.00", límite 5 y motivo "Nuevo precio"
    Y el dueño retira el plan "Plan Base" con motivo "Reemplazado por Plan Base 2"
    Entonces la suscripción de la empresa conserva el plan "Plan Base" con precio "80.00"

  Escenario: Retiro y nueva publicación con auditoría
    Dado que el dueño registró el plan "Plan Temporal" con precio mensual "60.00", límite 3 y motivo "Promoción de temporada"
    Cuando el dueño retira el plan "Plan Temporal" con motivo "Fin de la promoción"
    Entonces la respuesta tiene código 200
    Y las empresas no ven el plan "Plan Temporal" en el catálogo
    Cuando el dueño vuelve a publicar el plan "Plan Temporal" con motivo "Promoción extendida"
    Entonces la respuesta tiene código 200
    Y las empresas ven el plan "Plan Temporal" en el catálogo
    Y la auditoría del catálogo registra 3 cambios

  Esquema del escenario: Rechazar un plan con datos inválidos
    Cuando el dueño registra el plan "Plan Inválido" con precio mensual "<precio>", límite <límite> y motivo "<motivo>"
    Entonces la respuesta tiene código 400
    Y no se registra el plan "Plan Inválido"

    Ejemplos:
      | precio | límite | motivo                |
      | 0      | 5      | Precio sin valor      |
      | 50.00  | 0      | Límite sin unidades   |
      | 50.00  | 5      |                       |
