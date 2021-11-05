FROM amazon/aws-cli:2.1.1

ENV AWS_DEFAULT_REGION=us-east-2
ENV AWS_ACCESS_KEY_ID=FAKE
ENV AWS_SECRET_ACCESS_KEY=FAKE

WORKDIR /root

COPY initialize .
COPY public-key .
COPY private-key .
COPY epic-poc-private-key .

ENTRYPOINT []
CMD ["sh", "-c", "./initialize"]
