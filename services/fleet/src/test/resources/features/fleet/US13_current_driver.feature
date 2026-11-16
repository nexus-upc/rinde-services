# language: es
Característica: US13 Consultar la ficha del conductor vinculado a mi cuenta
  Como conductor
  Quiero consultar la ficha con la que mi empresa me registró
  Para confirmar que mi cuenta está vinculada a mi conductor

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión

  Regla: El conductor consulta solo la ficha vinculada a su propia cuenta

    Escenario: Un conductor vinculado obtiene su ficha
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "luis@andes.pe" con el rol "DRIVER"
      Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31" vinculado al usuario "luis@andes.pe"
      Y que el conductor "luis@andes.pe" inició sesión
      Cuando el conductor "luis@andes.pe" consulta su ficha de conductor
      Entonces la respuesta tiene código 200
      Y la respuesta incluye el campo "licenseNumber" con el valor "Q10203040"
      Y la respuesta vincula la cuenta de "luis@andes.pe"

    Escenario: Un conductor sin vínculo recibe 404
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que el administrador registró el conductor "Otro Chofer" con documento "DNI" "44556677", licencia "Q44556677" categoría "A-IIb" y vencimiento "2028-12-31"
      Y que el conductor "luis@andes.pe" inició sesión
      Cuando el conductor "luis@andes.pe" consulta su ficha de conductor
      Entonces la respuesta tiene código 404
      Y el mensaje de error es "La cuenta no está vinculada a ningún conductor."

    Escenario: Un administrador no tiene ficha de conductor
      Cuando el administrador consulta su ficha de conductor
      Entonces la respuesta tiene código 403
