require('./config');
const app = require('./app');
const db = require('./db');

const PORT = Number(process.env.PORT) || 3000;

async function start() {
  try {
    await db.query('SELECT 1');
    console.log('Conectado a MySQL');
  } catch (err) {
    console.error(`No se pudo conectar a MySQL (${err.code || err.message}). Revisa las variables DB_* en .env`);
    process.exit(1);
  }

  app.listen(PORT, () => {
    console.log(`Servidor corriendo en http://localhost:${PORT}`);
  });
}

start();
