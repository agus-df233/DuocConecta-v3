-- Prompts de ejemplo. SOLO se aplica con el perfil `dev`.
-- Los autores coinciden con los perfiles demo de ms-usuarios.

INSERT INTO prompt (id, titulo, proposito, contenido, categoria, carrera, herramienta, autor_id, visibilidad,
                    bifurcaciones, suma_estrellas, cantidad_valoraciones) VALUES
    ('cccccccc-0000-0000-0000-000000000001', 'Explícame un error sin hacerme la tarea',
     'Pegas el error y el código: te explica qué pasa y te guía para arreglarlo tú.',
     'Actúa como profesor de programación. Te voy a pegar un mensaje de error y el código que lo produce. '
     || 'Explícame en palabras simples qué significa el error, en qué línea está el problema y por qué pasa. '
     || 'No me des el código corregido: dame pistas en orden, de la más general a la más específica.',
     'CODIGO', 'Ingeniería en Informática', 'ChatGPT', 'demo-oid-0001', 'PUBLICO', 1, 9, 2),

    ('cccccccc-0000-0000-0000-000000000002', 'Brief creativo para una pieza gráfica',
     'Convierte una idea suelta en un brief con público, tono, formato y referencias.',
     'Eres director de arte. Con esta idea: [idea], arma un brief con: objetivo, público, mensaje principal, '
     || 'tono, formatos de salida, paleta sugerida y tres referencias visuales que debería buscar.',
     'DISENO', 'Diseño Gráfico', 'Claude', 'demo-oid-0002', 'PUBLICO', 0, 5, 1),

    ('cccccccc-0000-0000-0000-000000000003', 'Resumen de un paper para estudiar',
     'Resume un texto académico en ideas clave, conceptos y preguntas de repaso.',
     'Resume el siguiente texto para estudiar. Dame: 1) la idea central en dos líneas, 2) cinco conceptos '
     || 'clave con su definición, 3) cinco preguntas de repaso con respuesta corta. Texto: [pega aquí]',
     'ESTUDIO', NULL, 'Gemini', 'demo-oid-0003', 'PUBLICO', 0, 0, 0);
