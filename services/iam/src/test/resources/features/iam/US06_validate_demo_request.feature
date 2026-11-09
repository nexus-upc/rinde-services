# language: es
Característica: US06 Validación de la solicitud de demostración
  Como visitante de la landing page
  Quiero que el formulario revise mis datos antes de enviarlos
  Para corregir los campos obligatorios sin que se registre una solicitud incompleta

  Esquema del escenario: Campo obligatorio vacío o con formato inválido
    Dado que el visitante completa la solicitud de demostración con datos válidos
    Y el campo "<campo>" tiene el valor "<valor>"
    Cuando el visitante envía la solicitud de demostración
    Entonces la respuesta tiene código 400
    Y el error del campo "<campo>" es "<mensaje>"
    Y no se registra ninguna solicitud de demostración

    Ejemplos:
      | campo       | valor             | mensaje                                           |
      | fullName    |                   | El nombre es obligatorio.                         |
      | companyName |                   | La empresa es obligatoria.                        |
      | email       |                   | El correo es obligatorio.                         |
      | email       | correo-sin-arroba | Ingresa un correo válido.                         |
      | fleetSize   |                   | La cantidad de unidades es obligatoria.          |
      | fleetSize   | 0                 | La cantidad de unidades debe estar entre 1 y 500. |
      | fleetSize   | 501               | La cantidad de unidades debe estar entre 1 y 500. |
      | phone       | abc               | El teléfono no tiene un formato válido.           |

  Escenario: Mensaje de más de 500 caracteres
    Dado que el visitante completa la solicitud de demostración con datos válidos
    Y el mensaje tiene 501 caracteres
    Cuando el visitante envía la solicitud de demostración
    Entonces la respuesta tiene código 400
    Y el error del campo "message" es "El mensaje no puede superar los 500 caracteres."
    Y no se registra ninguna solicitud de demostración
