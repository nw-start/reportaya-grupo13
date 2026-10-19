import os

from dotenv import load_dotenv
from sqlalchemy import URL, create_engine
from sqlalchemy.engine import make_url
from sqlalchemy.orm import DeclarativeBase, sessionmaker


load_dotenv()


def build_database_url():
    host = os.getenv("DB_HOST")

    if host:
        return URL.create(
            "mysql+pymysql",
            username=os.getenv("DB_USER"),
            password=os.getenv("DB_PASSWORD"),
            host=host,
            port=int(os.getenv("DB_PORT", "3306")),
            database=os.getenv("DB_NAME", "defaultdb"),
        )

    database_url = os.getenv("DATABASE_URL", "sqlite:///./reportaya_dev.db")
    url = make_url(database_url)

    if url.drivername == "mysql":
        url = url.set(drivername="mysql+pymysql")

    if "ssl-mode" in url.query:
        url = url.difference_update_query(["ssl-mode"])

    return url


DATABASE_URL = build_database_url()

engine_options = {"pool_pre_ping": True}

if DATABASE_URL.get_backend_name() == "sqlite":
    engine_options["connect_args"] = {"check_same_thread": False}
elif DATABASE_URL.get_backend_name() == "mysql":
    engine_options["pool_recycle"] = 300
    engine_options["connect_args"] = {"ssl": {"check_hostname": False}}

engine = create_engine(DATABASE_URL, **engine_options)
SessionLocal = sessionmaker(bind=engine, autocommit=False, autoflush=False)


class Base(DeclarativeBase):
    pass


def get_database():
    database = SessionLocal()
    try:
        yield database
    finally:
        database.close()
