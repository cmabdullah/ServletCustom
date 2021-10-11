//package com;
//
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//
//public class DefaultWebClient {
//	public static void main(String[] args) {
//
//		System.out.println("CM");
//		WebClient client = WebClient.create();
//
//
//		String mono = client.get()
//				.uri("http://localhost:8080/api/v1/product/add?a1=cm&a2=khan")
//				.retrieve()
//				/*.onStatus(httpStatus -> HttpStatus.NOT_FOUND.equals(httpStatus),
//						clientResponse -> Mono.empty())*/
//				.bodyToMono(String.class).block();
//
//		System.out.println(mono);
//
//	}
//}
