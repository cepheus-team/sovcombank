# 
FROM python:3.10-alpine

WORKDIR /code

COPY ./requirements.txt /code/requirements.txt

RUN apk add build-base
RUN pip install --no-cache-dir --upgrade -r /code/requirements.txt

COPY ./app /code/app

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "80", "--proxy-headers"]