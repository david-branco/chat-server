## Chat Server
Project developed under the discipline of Paradigms of Distributed Systems of the Master's Degree in Informatics Engineering (University of Minho).<br>
This project consists of implementing a Chat Server in Java using 3 different paradigms of distributed systems: actors, message-oriented and resource-oriented.

*Used Technologies: Java, Quasar, ZeroMQ, Dropwizard, Telnet, Gradle, Apache Maven, IntelliJ IDEA, GitHub, Trello, LaTeX, Texmaker, among others.*

The system has among its main functionalities:
- Authentication service for the users and administrators;
- Messages exchange in private and in public rooms;
- Simple text-based protocol that allows the message exchange  between clients through telnet;
- Management and description of chat components through a REST API (e.g. room creation/removal, list of users in the room, etc.);
- Subscribing to relevant chat events using a notification API (e.g. room creation/removal, user joining/leaving room, etc.);
- Scalable implementation regarding the number of connected users;
- Among other features.

For an Administrator it is possible:
- Manage the Rooms: consult Information, create, delete and list the information regarding all the rooms as well as the users in them;
- Manage Users: consult Information, delete Account, expel from a room and list information for all users with account in the system;
- Among other features.

For a chat User it is possible:
- Consult, send and receive private messages to/from other users;
- Consult information regarding the rooms and users present;
- Enter a chat room and exchange messages with its members;
- Login through email and password;
- Registration of an account through a nickname, email and password;
- Account management: consult, edit and delete information;
- Among other features.

Representative Models: [Servers](https://raw.githubusercontent.com/david-branco/chat-server/master/diagrams/servers.png), [Server Actors](https://raw.githubusercontent.com/david-branco/chat-server/master/diagrams/server_actors.png), [Acceptor Admin](https://raw.githubusercontent.com/david-branco/chat-server/master/diagrams/acceptor_admin.png), [Acceptor User](https://raw.githubusercontent.com/david-branco/chat-server/master/diagrams/acceptor_user.png), [Admin](https://raw.githubusercontent.com/david-branco/chat-server/master/diagrams/admin.png), [User](https://raw.githubusercontent.com/david-branco/chat-server/master/diagrams/user.png).

---

## Servidor de Chat
Projecto desenvolvido no âmbito da disciplina de Paradigmas de Sistemas Distribuídos do Mestrado em Engenharia Informática (Universidade do Minho).<br>
Este projecto consiste na implementação de um servidor de chat em Java utilizando 3 paradigmas diferentes de sistemas distribuidos: actors, message-oriented and resource-oriented. 

*Tecnologias Utilizadas: Java, Quasar, ZeroMQ, Dropwizard, Telnet, Gradle, Apache Maven, IntelliJ IDEA, GitHub, Trello, LaTeX, Texmaker entre outras.*

O sistema possui entre as suas funcionalidades principais:
- Serviço de autenticação para Utilizadores e Administradores;
- Troca de mensagens em privado e em salas publicas;	
- Protocolo simples text-based que permite a troca de mensagens entre clientes através de telnet;
- Gestão e descrição de componentes do chat através de uma API REST (e.g. criação/remoção de salas, listar utilizadores numa sala, etc.);
- Subscrição de eventos relevantes no chat recorrendo a uma API de notificação (e.g. criação/remoção de salas, entrada/saída de utilizadores de salas, etc.);
- Implementação escalável relativamente ao número de utilizadores conectados;
- Entre outras funcionalidades.

Para um Administrador é possivel:
- Gerir as Salas: consultar informação, criar, apagar e listar a informação referente a todas as salas assim como os utilizadores nelas presentes;
- Gerir os Utilizadores: consultar informação, apagar Conta, expulsar de uma sala e listar a informação referente a todos os utilizadores com conta no sistema;
- Entre outras funcionalidades.

Para um Utilizador do chat é possivel:
- Consultar, enviar e receber mensagens privadas de/para outros utilizadores;
- Consultar informação referente às salas e utilizadores presentes;
- Entrar numa sala de chat e trocar mensagens com os seus intervenientes;
- Login através do seu email e password;
- Registo de uma conta através de um nickname, email e password;
- Gestão da sua conta: consultar, editar e apagar informação;
- Entre outras funcionalidades.

Modelos Representativos: [Servidores](https://raw.githubusercontent.com/david-branco/chat-server/master/diagrams/servers.png), [Servidor Actors](https://raw.githubusercontent.com/david-branco/chat-server/master/diagrams/server_actors.png), [Acceptor Admin](https://raw.githubusercontent.com/david-branco/chat-server/master/diagrams/acceptor_admin.png), [Acceptor User](https://raw.githubusercontent.com/david-branco/chat-server/master/diagrams/acceptor_user.png), [Admin](https://raw.githubusercontent.com/david-branco/chat-server/master/diagrams/admin.png), [User](https://raw.githubusercontent.com/david-branco/chat-server/master/diagrams/user.png).