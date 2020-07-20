package c.jbd.webflux.functional;

import c.jbd.webflux.data.mongo.Contact;
import c.jbd.webflux.data.mongo.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ContactRestHandler {
  private final ContactRepository contactRepository;

  private Mono<ServerResponse> response404
    = ServerResponse.notFound().build();

  private Mono<ServerResponse> response406
    = ServerResponse.status(HttpStatus.NOT_ACCEPTABLE).build();

  @Autowired //optional
  public ContactRestHandler(ContactRepository contactRepository) {
    this.contactRepository = contactRepository;
  }

  public Mono<ServerResponse> getAllContacts(ServerRequest request) {
    return ServerResponse.ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body(contactRepository.findAll(), Contact.class);
  }

  public Mono<ServerResponse> getById(ServerRequest request) {
    String id = request.pathVariable("id");

    return contactRepository.findById(id)
      .flatMap(contact ->
        ServerResponse.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(contact, Contact.class)
      ).switchIfEmpty(response404);
  }

  public Mono<ServerResponse> getByEmail(ServerRequest request) {
    String email = request.pathVariable("email");

    return contactRepository.findFirstByEmail(email)
      .flatMap(contact ->
        ServerResponse.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(contact, Contact.class)
      ).switchIfEmpty(response404);
  }

  public Mono<ServerResponse> insertContact(ServerRequest request) {
    Mono<Contact> unsavedContact = request.bodyToMono(Contact.class);

    return unsavedContact
      .flatMap(contact -> contactRepository.save(contact)
        .flatMap(savedContact -> ServerResponse.accepted()
          .contentType(MediaType.APPLICATION_JSON)
          .body(savedContact, Contact.class))
      ).switchIfEmpty(response406);
  }

  public Mono<ServerResponse> updateContact(ServerRequest request) {
    Mono<Contact> contact$ = request.bodyToMono(Contact.class);
    String id = request.pathVariable("id");

    //TODO - additional id match
    Mono<Contact> updatedContact$ = contact$.flatMap(contact ->
      contactRepository.findById(id)
        .flatMap(oldContact -> {
          oldContact
            .setPhone(contact.getPhone())
            .setName(contact.getName())
            .setEmail(contact.getEmail());
          return contactRepository.save(oldContact);
        })
    );

    return updatedContact$.flatMap(contact ->
      ServerResponse.accepted()
        .contentType(MediaType.APPLICATION_JSON)
        .body(contact, Contact.class)
    ).switchIfEmpty(response404);
  }

  public Mono<ServerResponse> deleteContact(ServerRequest request){
    String id = request.pathVariable("id");
    Mono<Void> deleted = contactRepository.deleteById(id);

    return ServerResponse.ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body(deleted, Void.class);
  }
}
