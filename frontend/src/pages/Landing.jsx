import { Link } from 'react-router-dom';
import { Container, Nav, Navbar, Row, Col } from 'react-bootstrap';
import { useAuth } from '../AuthContext';
import { HOME } from '../api';
import { Img } from '../components/ui';

const U = (id) => `https://images.unsplash.com/${id}?auto=format&fit=crop&w=800&q=70`;

const PASOS = [
  { n: '01', t: 'Regístrate', d: 'Crea tu cuenta como paciente, o como tutor/apoderado si gestionarás la atención de un familiar.', img: U('photo-1551076805-e1869033e561') },
  { n: '02', t: 'Elige especialidad y horario', d: 'Selecciona Psiquiatría o Psicología, el día y el turno (mañana o tarde) que mejor se ajuste.', img: U('photo-1584515933487-779824d29309') },
  { n: '03', t: 'Recibe tu confirmación', d: 'Te avisamos por notificación y correo, y un día antes te enviamos un recordatorio.', img: U('photo-1576091160399-112ba8d25d1d') },
];

const OFRECEMOS = [
  { i: 'bi-activity', t: 'Psiquiatría', d: 'Evaluación, diagnóstico y tratamiento de trastornos mentales con manejo farmacológico.', a: '#especialidades' },
  { i: 'bi-chat-heart', t: 'Psicología', d: 'Psicoterapia y acompañamiento emocional con profesionales especializados.', a: '#especialidades' },
  { i: 'bi-people', t: 'Tutor / Apoderado', d: 'Un familiar o responsable puede gestionar las citas y recibir avisos por el paciente.', a: '#tutor' },
  { i: 'bi-capsule', t: 'Farmacia', d: 'Consulta y recojo de tus medicamentos recetados en el mismo hospital.', a: '#farmacia' },
];

export default function Landing() {
  const { usuario } = useAuth();
  const destino = usuario ? HOME[usuario.rol] : '/registro';

  return (
    <>
      <div className="topbar d-none d-md-block">
        <Container className="d-flex justify-content-between flex-wrap gap-2">
          <span>
            <i className="bi bi-telephone me-1" /> (01) 000-0000 &nbsp;·&nbsp; <i className="bi bi-envelope me-1" />
            <a href="mailto:citas@valdizan.gob.pe">citas@valdizan.gob.pe</a> &nbsp;·&nbsp; <i className="bi bi-clock me-1" /> Lun–Sáb 08:00–19:00
          </span>
          <span className="d-flex gap-3">
            <a href="#!" aria-label="Facebook"><i className="bi bi-facebook" /></a>
            <a href="#!" aria-label="Instagram"><i className="bi bi-instagram" /></a>
            <a href="#!" aria-label="YouTube"><i className="bi bi-youtube" /></a>
          </span>
        </Container>
      </div>

      <Navbar expand="lg" className="navbar-valdizan sticky-top" collapseOnSelect>
        <Container>
          <Navbar.Brand as={Link} to="/" className="fw-bold" style={{ color: 'var(--azul-osc)' }}>
            <i className="bi bi-hospital me-2" />Hospital Hermilio Valdizán
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="nav-landing" />
          <Navbar.Collapse id="nav-landing">
            <Nav className="ms-auto align-items-lg-center gap-lg-2">
              <Nav.Link href="#bienvenida">Inicio</Nav.Link>
              <Nav.Link href="#especialidades">Especialidades</Nav.Link>
              <Nav.Link href="#como">Cómo reservar</Nav.Link>
              <Nav.Link href="#tutor">Tutor</Nav.Link>
              <Nav.Link href="#farmacia">Farmacia</Nav.Link>
              <Nav.Link href="#ubicacion">Ubicación</Nav.Link>
              {usuario ? (
                <Link to={HOME[usuario.rol]} className="btn btn-azul btn-sm">Ir a mi panel</Link>
              ) : (
                <>
                  <Link to="/login" className="btn btn-outline-azul btn-sm">Ingresar</Link>
                  <Link to="/registro" className="btn btn-azul btn-sm">Registrarme</Link>
                </>
              )}
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>

      <section className="hero" id="inicio">
        <Container className="text-center">
          <h1>Tu salud mental,<br />en buenas manos</h1>
          <p className="lead mx-auto my-4" style={{ maxWidth: 640 }}>
            Reserva tu cita de psiquiatría o psicología en pocos pasos, de forma simple y segura.
          </p>
          <Link to={destino} className="btn btn-hero">Reservar cita</Link>
        </Container>
      </section>

      <section className="seccion" id="bienvenida">
        <Container className="text-center" style={{ maxWidth: 820 }}>
          <h2>Bienvenido al Hospital Hermilio Valdizán</h2>
          <div className="acento" />
          <p className="fs-5 text-secondary">
            Somos un hospital especializado en salud mental. Con este sistema puedes pedir, reprogramar o cancelar tus citas de consulta externa
            desde cualquier dispositivo, sin hacer largas colas, y recibir avisos de tu atención.
          </p>
        </Container>
      </section>

      <section className="seccion seccion-gris" id="especialidades">
        <Container>
          <h2 className="text-center">Qué ofrecemos</h2>
          <div className="acento" />
          <Row className="g-4">
            {OFRECEMOS.map((o) => (
              <Col md={6} lg={3} key={o.t}>
                <div className="tarjeta text-center p-4 d-flex flex-column">
                  <div className="icono-circulo"><i className={`bi ${o.i}`} /></div>
                  <h3 className="h5">{o.t}</h3>
                  <p className="text-secondary flex-grow-1">{o.d}</p>
                  <a href={o.a} className="btn btn-azul btn-sm align-self-center">Ver más</a>
                </div>
              </Col>
            ))}
          </Row>
        </Container>
      </section>

      <section className="seccion" id="como">
        <Container>
          <h2 className="text-center">Cómo reservar tu cita</h2>
          <div className="acento" />
          <Row className="g-4">
            {PASOS.map((p) => (
              <Col md={4} key={p.n}>
                <div className="tarjeta pb-4">
                  <div className="tarjeta-img"><Img src={p.img} alt="" /><div className="circulo-num">{p.n}</div></div>
                  <div className="p-4 pt-5 text-center">
                    <h3 className="h5">{p.t}</h3>
                    <p className="text-secondary mb-0">{p.d}</p>
                  </div>
                </div>
              </Col>
            ))}
          </Row>
        </Container>
      </section>

      <section className="seccion seccion-gris" id="tutor">
        <Container>
          <Row className="align-items-center g-5">
            <Col lg={6}>
              <h2>Tutor / Apoderado: acompañamos a quienes necesitan apoyo</h2>
              <div className="acento ms-0" />
              <p className="text-secondary fs-5">
                Muchos pacientes no pueden gestionar su atención por sí solos. Por eso, un familiar o responsable legal puede registrarse como tutor
                y actuar en nombre del paciente.
              </p>
              <ul className="list-unstyled fs-6">
                <li className="mb-2"><i className="bi bi-check-circle-fill text-primary me-2" />Reservar, reprogramar y cancelar citas por el paciente.</li>
                <li className="mb-2"><i className="bi bi-check-circle-fill text-primary me-2" />Recibir las notificaciones y recordatorios de cada cita.</li>
                <li className="mb-2"><i className="bi bi-check-circle-fill text-primary me-2" />Consultar el historial de atención en modo lectura.</li>
                <li className="mb-2"><i className="bi bi-check-circle-fill text-primary me-2" />Gestionar a varios pacientes desde una sola cuenta.</li>
              </ul>
              <Link to="/registro?tipo=TUTOR" className="btn btn-azul">Registrarme como tutor</Link>
            </Col>
            <Col lg={6}>
              <div className="tarjeta"><div className="tarjeta-img" style={{ height: 320 }}><Img src={U('photo-1573496359142-b8d87734a5a2')} alt="Familiar acompañando a un paciente" /></div></div>
            </Col>
          </Row>
        </Container>
      </section>

      <section className="seccion" id="farmacia">
        <Container>
          <Row className="align-items-center g-5">
            <Col lg={6} className="order-lg-2">
              <h2>Farmacia: consulta y recojo de tus medicamentos recetados</h2>
              <div className="acento ms-0" />
              <p className="text-secondary fs-5">
                Cuando tu médico emite una receta, esta llega directamente a la farmacia del hospital. Un farmacéutico verifica el stock y te entrega
                tus medicamentos, de forma total o parcial según la disponibilidad.
              </p>
            </Col>
            <Col lg={6} className="order-lg-1">
              <div className="tarjeta"><div className="tarjeta-img" style={{ height: 300 }}><Img src={U('photo-1587854692152-cbe660dbde88')} alt="Medicamentos en farmacia" /></div></div>
            </Col>
          </Row>
        </Container>
      </section>

      <section className="seccion seccion-gris" id="ubicacion">
        <Container className="text-center" style={{ maxWidth: 760 }}>
          <h2>¿Dónde estamos?</h2>
          <div className="acento" />
          <p className="fs-5 mb-1"><i className="bi bi-geo-alt-fill text-primary me-2" />Av. Mendiburu, Ate / Santa Eugenia, Lima</p>
          <p className="text-muted small">Ubicación referencial. Al reservar tu cita verás el pabellón y room exactos de tu consultorio.</p>
        </Container>
      </section>

      <section className="banda-cta text-center">
        <Container>
          <h2>Tu bienestar es lo primero</h2>
          <p className="fs-5 mb-4">Crea tu cuenta y reserva tu primera cita hoy mismo.</p>
          <Link to={destino} className="btn btn-hero">Reservar cita</Link>
        </Container>
      </section>

      <footer className="footer">
        <Container>
          <Row className="g-4">
            <Col md={5}>
              <h6>Hospital Hermilio Valdizán</h6>
              <p className="mb-1">Hospital especializado en salud mental. Consulta externa: psiquiatría y psicología.</p>
              <p className="small">Sus datos personales se tratan conforme a la Ley N.° 29733 de Protección de Datos Personales.</p>
            </Col>
            <Col md={3}>
              <h6>Enlaces</h6>
              <div><Link to="/login">Ingresar</Link></div>
              <div><Link to="/registro">Registrarme</Link></div>
              <div><a href="#como">Cómo reservar</a></div>
            </Col>
            <Col md={4}>
              <h6>Contacto</h6>
              <div>citas@valdizan.gob.pe</div>
              <div>Lun–Sáb 08:00–19:00</div>
            </Col>
          </Row>
          <hr className="border-secondary" />
          <div className="text-center small">© {new Date().getFullYear()} Hospital Hermilio Valdizán — Sistema de gestión de citas. Datos de demostración.</div>
        </Container>
      </footer>
    </>
  );
}
