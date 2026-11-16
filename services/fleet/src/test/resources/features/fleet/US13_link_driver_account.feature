# language: es
Característica: US13 Vincular la cuenta DRIVER de la empresa a un conductor
  Como administrador o responsable de operaciones
  Quiero vincular a cada conductor con su cuenta DRIVER de la empresa
  Para que el conductor opere desde su propia cuenta

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión

  Regla: La cuenta vinculada debe ser DRIVER y pertenecer a la misma empresa

    Escenario: Vincular una cuenta DRIVER de la empresa al registrar al conductor
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "luis@andes.pe" con el rol "DRIVER"
      Cuando el administrador registra el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31" vinculado al usuario "luis@andes.pe"
      Entonces la respuesta tiene código 201
      Y la respuesta vincula la cuenta de "luis@andes.pe"
      Y el conductor queda con el estado "ENABLED"

    Escenario: Rechazar una cuenta que pertenece a otra empresa
      Dado que existe una empresa verificada con RUC "20987654321" y administrador "otro@otra.pe"
      Y que el administrador "otro@otra.pe" inició sesión
      Y que el administrador invitó a "Chofer Otra" con el correo "chofer@otra.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "chofer@otra.pe" con el rol "DRIVER"
      Y que el administrador "ana@andes.pe" inició sesión
      Cuando el administrador registra el conductor "Chofer Otra" con documento "DNI" "77889900", licencia "Q77889900" categoría "A-IIIb" y vencimiento "2028-12-31" vinculado al usuario "chofer@otra.pe"
      Entonces la respuesta tiene código 404
      Y el mensaje de error es "La cuenta no existe en la empresa."

    Escenario: Rechazar una cuenta con un rol distinto de DRIVER
      Dado que el administrador invitó a "Rosa Mendez" con el correo "rosa@andes.pe" y el rol "OPERATIONS_MANAGER"
      Y que IAM informa la cuenta de "rosa@andes.pe" con el rol "OPERATIONS_MANAGER"
      Cuando el administrador registra el conductor "Rosa Mendez" con documento "DNI" "66554433", licencia "Q66554433" categoría "A-IIb" y vencimiento "2028-12-31" vinculado al usuario "rosa@andes.pe"
      Entonces la respuesta tiene código 400
      Y el mensaje de error es "La cuenta debe tener el rol de conductor."

  Regla: Una cuenta solo puede estar vinculada a un conductor

    Escenario: Rechazar una cuenta que ya está vinculada a otro conductor
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "luis@andes.pe" con el rol "DRIVER"
      Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31" vinculado al usuario "luis@andes.pe"
      Cuando el administrador registra el conductor "Luis Otra Vez" con documento "DNI" "88776655", licencia "Q88776655" categoría "A-IIIc" y vencimiento "2028-12-31" vinculado al usuario "luis@andes.pe"
      Entonces la respuesta tiene código 409
      Y el mensaje de error es "La cuenta ya está vinculada a otro conductor."

  Regla: La cuenta debe estar activa para vincularse, al registrar y al cambiar

    Escenario: Rechazar al alta una cuenta invitada que aún no activó
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "luis@andes.pe" con el rol "DRIVER" y el estado "INVITED"
      Cuando el administrador registra el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31" vinculado al usuario "luis@andes.pe"
      Entonces la respuesta tiene código 409
      Y el mensaje de error es "La cuenta del conductor no está activa."

    Escenario: Rechazar al alta una cuenta deshabilitada
      Dado que el administrador invitó a "Chofer Baja" con el correo "baja@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "baja@andes.pe" con el rol "DRIVER" y el estado "DISABLED"
      Cuando el administrador registra el conductor "Chofer Baja" con documento "DNI" "21304050", licencia "Q21304050" categoría "A-IIb" y vencimiento "2028-12-31" vinculado al usuario "baja@andes.pe"
      Entonces la respuesta tiene código 409
      Y el mensaje de error es "La cuenta del conductor no está activa."

    Escenario: Rechazar un cambio a una cuenta deshabilitada
      Dado que el administrador invitó a "Chofer Baja" con el correo "baja@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "baja@andes.pe" con el rol "DRIVER" y el estado "DISABLED"
      Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31"
      Cuando "ana@andes.pe" asigna la cuenta "baja@andes.pe" al conductor "Luis Quispe"
      Entonces la respuesta tiene código 409
      Y el mensaje de error es "La cuenta del conductor no está activa."

  Regla: El administrador o el responsable de operaciones cambian o retiran el vínculo de un conductor

    Escenario: El administrador cambia el vínculo a otra cuenta DRIVER activa
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "luis@andes.pe" con el rol "DRIVER"
      Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31" vinculado al usuario "luis@andes.pe"
      Y que el administrador invitó a "Carla Ruiz" con el correo "carla@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "carla@andes.pe" con el rol "DRIVER"
      Cuando "ana@andes.pe" asigna la cuenta "carla@andes.pe" al conductor "Luis Quispe"
      Entonces la respuesta tiene código 200
      Y la respuesta vincula la cuenta de "carla@andes.pe"

    Escenario: El responsable de operaciones cambia el vínculo
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "luis@andes.pe" con el rol "DRIVER"
      Y que el administrador invitó a "Rosa Mendez" con el correo "rosa@andes.pe" y el rol "OPERATIONS_MANAGER"
      Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31"
      Cuando "rosa@andes.pe" asigna la cuenta "luis@andes.pe" al conductor "Luis Quispe"
      Entonces la respuesta tiene código 200
      Y la respuesta vincula la cuenta de "luis@andes.pe"

    Escenario: El administrador retira el vínculo de un conductor
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "luis@andes.pe" con el rol "DRIVER"
      Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31" vinculado al usuario "luis@andes.pe"
      Cuando "ana@andes.pe" retira la cuenta del conductor "Luis Quispe"
      Entonces la respuesta tiene código 200
      Y la respuesta no tiene cuenta vinculada

    Escenario: Volver a vincular la misma cuenta no cambia el conductor
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "luis@andes.pe" con el rol "DRIVER"
      Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31" vinculado al usuario "luis@andes.pe"
      Cuando "ana@andes.pe" asigna la cuenta "luis@andes.pe" al conductor "Luis Quispe"
      Entonces la respuesta tiene código 200
      Y la respuesta vincula la cuenta de "luis@andes.pe"

    Escenario: Rechazar el cambio a una cuenta ya vinculada a otro conductor
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "luis@andes.pe" con el rol "DRIVER"
      Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31" vinculado al usuario "luis@andes.pe"
      Y que el administrador registró el conductor "Otro Chofer" con documento "DNI" "44556677", licencia "Q44556677" categoría "A-IIb" y vencimiento "2028-12-31"
      Cuando "ana@andes.pe" asigna la cuenta "luis@andes.pe" al conductor "Otro Chofer"
      Entonces la respuesta tiene código 409
      Y el mensaje de error es "La cuenta ya está vinculada a otro conductor."

    Escenario: Rechazar el cambio a una cuenta de otra empresa
      Dado que existe una empresa verificada con RUC "20987654321" y administrador "otro@otra.pe"
      Y que el administrador "otro@otra.pe" inició sesión
      Y que el administrador invitó a "Chofer Otra" con el correo "chofer@otra.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "chofer@otra.pe" con el rol "DRIVER"
      Y que el administrador "ana@andes.pe" inició sesión
      Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31"
      Cuando "ana@andes.pe" asigna la cuenta "chofer@otra.pe" al conductor "Luis Quispe"
      Entonces la respuesta tiene código 404
      Y el mensaje de error es "La cuenta no existe en la empresa."

    Escenario: Rechazar el cambio a una cuenta con un rol distinto de DRIVER
      Dado que el administrador invitó a "Rosa Mendez" con el correo "rosa@andes.pe" y el rol "OPERATIONS_MANAGER"
      Y que IAM informa la cuenta de "rosa@andes.pe" con el rol "OPERATIONS_MANAGER"
      Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31"
      Cuando "ana@andes.pe" asigna la cuenta "rosa@andes.pe" al conductor "Luis Quispe"
      Entonces la respuesta tiene código 400
      Y el mensaje de error es "La cuenta debe tener el rol de conductor."

    Escenario: Un conductor no puede cambiar vínculos
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que IAM informa la cuenta de "luis@andes.pe" con el rol "DRIVER"
      Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31"
      Y que el conductor "luis@andes.pe" inició sesión
      Cuando "luis@andes.pe" asigna la cuenta "luis@andes.pe" al conductor "Luis Quispe"
      Entonces la respuesta tiene código 403
