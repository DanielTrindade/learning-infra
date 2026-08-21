run "porta_padrao_esta_na_faixa" {
  command = plan

  assert {
    condition     = docker_container.web.name == "mirante-web"
    error_message = "o container deveria se chamar mirante-web"
  }
}

run "porta_fora_da_faixa_reprova" {
  command = plan

  variables {
    porta = 9090
  }

  expect_failures = [
    docker_container.web,
  ]
}
