.PHONY: verify

DOCKER_HOST ?= $(shell podman machine inspect --format 'unix://{{.ConnectionInfo.PodmanSocket.Path}}')

verify:
	DOCKER_HOST=$(DOCKER_HOST) ./mvnw -B clean verify
