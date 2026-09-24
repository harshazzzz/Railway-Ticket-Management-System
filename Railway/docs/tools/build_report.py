"""Regenerate diagram PNGs and the coursework PDF. Requires Pillow and reportlab."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Image as RLImage, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from xml.sax.saxutils import escape

DOCS=Path(__file__).resolve().parent.parent
NAVY='#112339';TEAL='#007771';MUTED='#637284';BG='#F4F7FA'
def font(size,bold=False):
    candidates=[Path('C:/Windows/Fonts/'+('segoeuib.ttf' if bold else 'segoeui.ttf')),Path('/usr/share/fonts/truetype/dejavu/'+('DejaVuSans-Bold.ttf' if bold else 'DejaVuSans.ttf'))]
    return ImageFont.truetype(str(next(p for p in candidates if p.exists())),size)
def diagram(title,subtitle,boxes,edges,path):
    im=Image.new('RGB',(1800,1200),BG);d=ImageDraw.Draw(im)
    d.text((60,40),title,font=font(45,True),fill=NAVY);d.text((60,105),subtitle,font=font(24),fill=MUTED)
    for points,label in edges:
        d.line(points,fill=TEAL,width=4)
        x,y=points[-1];px,py=points[-2]
        if abs(x-px)>abs(y-py): tri=[(x,y),(x-12 if x>px else x+12,y-7),(x-12 if x>px else x+12,y+7)]
        else: tri=[(x,y),(x-7,y-12 if y>py else y+12),(x+7,y-12 if y>py else y+12)]
        d.polygon(tri,fill=TEAL)
    for x,y,w,h,name,lines in boxes:
        d.rounded_rectangle((x,y,x+w,y+h),radius=14,fill='white',outline='#D8E1E9',width=2)
        d.rounded_rectangle((x,y,x+w,y+55),radius=14,fill=NAVY)
        d.rectangle((x,y+30,x+w,y+55),fill=NAVY)
        d.text((x+18,y+10),name,font=font(26,True),fill='white')
        for i,line in enumerate(lines):d.text((x+18,y+70+i*33),line,font=font(23),fill=NAVY)
    for points,label in edges:
        if not label:continue
        a,b=max(zip(points,points[1:]),key=lambda pair:abs(pair[1][0]-pair[0][0])+abs(pair[1][1]-pair[0][1]))
        x,y=(a[0]+b[0])/2,(a[1]+b[1])/2
        if a[1]==b[1]:x-=d.textlength(label,font=font(21))/2;y-=28
        else:x+=12;y-=12
        d.text((x,y),label,font=font(21),fill=TEAL)
    d.text((60,1150),'RAILWAY  /  Java 17 + JDBC  /  Arrows indicate inheritance, implementation or labelled dependencies.',font=font(20),fill=MUTED)
    im.save(path)

diagram('Class design','Immutable model values, explicit user inheritance and polymorphic payments',[
 (60,190,500,195,'User  <<abstract>>',['- id, name, email, active','+ role()','+ dashboardTitle()']),
 (60,475,230,145,'Admin',['+ role()','+ dashboardTitle()']),
 (320,475,240,145,'Passenger',['+ role()','+ dashboardTitle()']),
 (650,190,440,195,'Payment  <<interface>>',['+ method()','+ authorize(amount)']),
 (610,475,260,145,'CardPayment',['approved: boolean','authorize(amount)']),
 (910,475,260,145,'CashPayment',['authorize(amount)']),
 (1280,190,450,195,'RailService',['login / register / logout','book / cancel / ticket','train & user CRUD']),
 (1280,475,450,145,'JdbcRepository',['read(work)','transaction(work)']),
 (60,790,290,195,'Train',['id, name, route','capacity, active']),
 (415,790,330,195,'Schedule',['id, Train, dates','fare, capacity','availableSeats']),
 (810,790,310,195,'Booking',['userId, scheduleId','status, seat, fare','ticket/payment view']),
 (1185,790,240,195,'Ticket',['bookingId, reference','seatNumber, price']),
 (1185,640,240,125,'Seat',['number, available']),
 (1490,790,250,195,'Transaction',['paymentId, type','amount, date'])
],[
 ([(155,475),(155,385)],'extends'), ([(385,475),(385,385)],'extends'),
 ([(740,475),(740,385)],'implements'), ([(1040,475),(1040,385)],'implements'),
 ([(1280,260),(1090,260)],'uses'), ([(1500,385),(1500,475)],'delegates'),
 ([(415,890),(350,890)],'has'), ([(810,900),(745,900)],'ID'),
 ([(1185,910),(1120,910)],'ID'),
],DOCS/'ClassDiagram.png')

diagram('Relational database','PK = primary key  /  FK = foreign key  /  UQ = unique  /  arrows point to referenced tables',[
 (60,200,440,220,'users',['PK id; UQ email','name, password_hash','role, active']),
 (680,200,440,220,'trains',['PK id','train_name, route','capacity, active']),
 (1300,200,440,220,'schedules',['PK id; FK train_id','departure, arrival, fare','capacity; UQ train + date']),
 (680,545,440,220,'bookings',['PK id; FK user_id','FK schedule_id','booking_date, status']),
 (60,880,440,210,'tickets',['PK id; UQ FK booking_id','seat_number, price','UQ reference']),
 (680,880,440,210,'payments',['PK id; UQ FK booking_id','amount, method, status']),
 (1300,545,440,220,'seat_allocations',['PK (schedule_id, seat)','FK schedule_id','UQ FK booking_id']),
 (1300,880,440,210,'payment_transactions',['PK id; FK payment_id','type, amount, created_at','UQ (payment_id, type)'])
],[
 ([(1300,300),(1120,300)],'N : 1'),
 ([(800,545),(800,465),(270,465),(270,420)],'N : 1'),
 ([(1050,545),(1050,485),(1530,485),(1530,420)],'N : 1'),
 ([(280,880),(280,680),(680,680)],'1 : 1'),
 ([(900,880),(900,765)],'1 : 1'),
 ([(1300,660),(1120,660)],'1 : 1'),
 ([(1560,545),(1560,420)],'N : 1'),
 ([(1300,990),(1120,990)],'N : 1'),
],DOCS/'DatabaseDiagram.png')

pages=[
('1. Introduction',[
 ('p','Railway is a standalone Java desktop Train Ticket Reservation System developed as an Object-Oriented Programming coursework and portfolio foundation. It models the passenger journey from account creation to train search, seat selection, simulated payment, ticket history and cancellation. Administrators operate the fleet, publish departures, maintain passenger accounts and inspect booking/payment activity.'),
 ('p','The objective is to demonstrate useful object-oriented design together with reliable data handling. The project uses Java 17, Swing, JDBC and MySQL, organized around MVC responsibilities. A separate H2 demo mode makes local evaluation possible without provisioning a server. This report describes the delivered implementation rather than a proposed system.'),
 ('h','Scope'),
 ('p','The application supports Admin and Passenger roles, one seat per booking, direct origin-to-destination journeys, LKR fares and a single local timezone. Card and cash payments are clearly labelled simulations; no payment credentials are accepted. Ticket Officer is optional in the brief and is not implemented.'),
 ('h','Learning outcomes'),
 ('p','The code demonstrates encapsulation, abstract classes, inheritance, interface-based polymorphism, immutable values, object relationships, CRUD, prepared SQL, transactions, exception handling, asynchronous GUI actions, testing and documentation. The viva guide supplies a source-reading order and a short demonstration script.')]),
('2. Problem Identification and Analysis',[
 ('p','A reservation system must coordinate scarce seats across users while keeping tickets and payments consistent. A basic CRUD interface cannot alone guarantee that two users do not book the same seat, that a failed payment releases capacity, or that one passenger cannot cancel another passenger\'s booking.'),
 ('p','Storing a single availability count on a train would mix journeys on different dates. Deleting user or train records would also break historical relationships. The design therefore separates fleet metadata, dated schedules, historical bookings and current seat allocations.'),
 ('h','3. Requirements Analysis'),
 ('table',[['Area','Delivered requirement'],['Authentication','Passenger registration, BCrypt login, logout and role checks'],['Passenger','Date/station search, seat choice, simulated payment, ticket history and cancellation'],['Admin','Train/user CRUD, departure creation, all bookings and payment reports'],['Integrity','Foreign keys, uniqueness, locking, rollback and idempotent refunds'],['Quality','MVC packages, immutable models, background workers, tests and documentation']]),
 ('p','Non-functional goals include a responsive desktop interface, readable source, safe error messages, repeatable tests and protected database credentials. The MySQL runtime user receives data access privileges without schema-administration rights. The interface must show that payments and tickets are coursework simulations.')]),
('4. System Design',[
 ('p','App is the composition root. MainFrame and Theme render Swing components. AppController executes work in SwingWorker and delivers results on the event-dispatch thread. RailService owns authentication, permissions and use cases. JdbcRepository binds parameters, runs connection-scoped callbacks and handles transactions. Database reads configuration and opens connections.'),
 ('table',[['Layer','Responsibility'],['View','Input, forms, tables, seat map and feedback'],['Controller','Background work and UI completion/error handling'],['Service','Business rules, authorization and transaction boundaries'],['Repository/database','Prepared JDBC, resource cleanup and connection settings'],['Model','Identity, train/schedule values, tickets and payment contracts']]),
 ('p','A page generation number stops an old asynchronous result from rendering into a new page after navigation. Business rules remain in the service so a hidden admin button is not the only permission boundary. Tests inject a repository and clock without starting the GUI.'),
 ('h','Booking transaction'),
 ('p','The service requires an active passenger, locks the train then schedule, validates seat and departure, inserts booking/ticket/allocation, invokes Payment.authorize and records payment/charge before commit. Any failure rolls back all records. The database also enforces a unique schedule-seat key. The coarse train lock intentionally favors understandable correctness over high-throughput scheduling.')]),
('5. Class Diagram',[
 ('image','ClassDiagram.png'),
 ('p','Admin and Passenger extend abstract User. The view invokes dashboardTitle through User, demonstrating dynamic dispatch. CardPayment and CashPayment implement the sealed Payment interface; booking invokes authorize through that contract. Schedule contains a Train. Booking is a read model combining related ticket, schedule and payment data for the view.'),
 ('p','Domain values are immutable. The traditional User class uses private final fields, while Java records concisely model Train, Schedule, Seat, Ticket, Booking and Transaction. RailService depends on JdbcRepository. Stored relationships are represented with stable IDs and enforced with foreign keys; the diagram highlights the central dependencies rather than every UI method.')]),
('6. Database Design',[
 ('image','DatabaseDiagram.png'),
 ('p','The schema contains users, trains, schedules, bookings, tickets, seat_allocations, payments and payment_transactions. application_locks is a separate one-row infrastructure table used to serialize first-admin bootstrap. It is omitted from the business relationship diagram.'),
 ('p','Available seats are derived from schedule capacity minus allocations. Cancellation retains booking/ticket/payment history and removes only active occupancy. Unique ticket/payment booking keys enforce one per booking; a unique payment/type pair prevents duplicate refund entries. Schedule capacity is a snapshot, and booked train details cannot be edited. Monetary values use DECIMAL(10,2).')]),
('7. OOP Concepts Used',[
 ('table',[['Concept','Application example'],['Encapsulation','User state is private/final; password hashes are not exposed in User.'],['Inheritance','Admin and Passenger share identity through User.'],['Polymorphism','dashboardTitle and Payment.authorize dispatch through abstract types.'],['Abstraction','User, Payment and JDBC callback contracts hide implementation details.'],['Classes/objects','Train, Schedule, Ticket, Booking, Seat and Transaction model the domain.'],['Relationships','Schedule contains Train; IDs link bookings, tickets and payments.']]),
 ('p','Sealed types express the deliberately limited role and payment variants. Records prevent accidental mutation after a query returns. Composition is used for services and repositories instead of inheriting from a generic framework base class. This keeps the design small enough to explain during a viva.'),
 ('h','Why the abstractions matter'),
 ('p','Payment polymorphism is used by a real business operation rather than included only for a diagram. The card simulation can decline, causing rollback; cash simulation approves a positive amount. The booking service does not branch on implementation type. User polymorphism likewise changes the dashboard heading through the shared type.')]),
('8. Implementation Details',[
 ('p','New passwords use BCrypt cost 12 with a minimum of 10 characters and a maximum of 72 UTF-8 bytes. Character arrays are cleared after processing; the BCrypt library still requires a Java String that cannot be erased deterministically. Email addresses are normalized. Registration always creates a passenger; admin bootstrap is an explicit terminal operation and rejects a second admin.'),
 ('p','Service-issued sessions are retained in memory and removed on logout. Each protected call re-reads the user and active state. Passenger booking lists are filtered by owner. Ticket and cancellation access also checks ownership. Prepared statements bind all user-controlled values. Connection, statement and result lifetimes use try-with-resources.'),
 ('p','Cancellation locks the schedule and booking, checks departure and ownership, updates status, releases allocation and writes a full simulated refund atomically. A second cancellation does nothing. Retiring a train with future confirmed reservations is rejected. Deactivating a user preserves historic bookings; administrator deactivation is blocked.'),
 ('h','UI workflow'),
 ('p','The split login page provides clear account actions. Sidebar navigation separates passenger search/history and administrative management/reports. Search results show route, time, capacity and fare. Seat buttons show unavailable positions, and payment choices explicitly identify simulation. Database work and password hashing run outside the Swing event-dispatch thread.'),
 ('h','Security boundary'),
 ('p','This application assumes a trusted workstation. Shared JDBC credentials cannot be hidden securely from a modified desktop client. A public deployment needs an authenticated server API, rate limiting and identity recovery. Real payments require a separate state machine and reconciliation rather than an external gateway call while holding database locks.')]),
('9. Testing',[
 ('p','The delivered service test suite contains 21 tests executed through actual JDBC with isolated H2 databases. All 21 passed with zero failures, errors or skipped tests during local validation. Java 23 compiled the source with --release 17. The Maven project and CI target Java 17; the local machine did not have a Java 17 runtime or MySQL server.'),
 ('table',[['Test group','Examples'],['Identity','Correct/incorrect login, hashing, normalization, duplicate email'],['Permissions','Role denial, logout, deactivated session, cross-account privacy'],['CRUD','Train/user changes, retirement and history protection'],['Reservations','Successful booking, seat bounds, duplicates and two-thread contention'],['Payments','Decline rollback, charge/refund balance and cancellation idempotency'],['Validation','Injection payloads, schedules, money and departed journeys']]),
 ('p','The concurrency test starts two workers competing for the same seat and requires exactly one success, one allocation and one booking. The decline test checks that no booking, ticket, payment, transaction or allocation survives rollback. A future clock verifies booking and cancellation rejection after departure.'),
 ('p','The same suite can target an isolated MySQL schema named railway_test. A GitHub Actions workflow provisions MySQL 8.4 and runs it after the H2 suite. MySQL CI and manual user-acceptance scenarios are supplied, not claimed as executed locally. See TESTING.md and machine-readable test evidence for exact results.')]),
('10. Conclusion',[
 ('p','The project goes beyond independent CRUD forms by connecting account permissions, dated seat inventory, transactions, payment behavior and preserved booking history. Its OOP examples are used in working flows, and its separation of concerns allows backend behavior to be tested independently from Swing.'),
 ('p','The implementation is suitable as a coursework foundation and portfolio demonstration when its limitations are presented honestly. Students should review the source, adapt the report with their own reasoning and follow institutional rules on acknowledging assistance. The included viva guide helps explain decisions but does not replace understanding the code.'),
 ('h','11. Future Improvements'),
 ('p','Priorities for a public system are an authenticated backend API, managed identity with recovery/MFA and rate limiting, and a real payment integration with idempotency and reconciliation. Operational improvements include schema migrations, connection pooling, paginated reports and audit events.'),
 ('p','Product extensions include multiple seats/passengers per booking, intermediate stops with segment-level occupancy, schedule editing with notification, ticket-officer verification and ticket export. Accessibility review, configurable currencies/timezones and larger-data performance tests would strengthen the desktop experience.'),
 ('h','Reference documentation'),
 ('p','MySQL Connector/J: https://dev.mysql.com/doc/connector-j/en/connector-j-installing-maven.html'),
 ('p','H2 features and compatibility: https://h2database.com/html/features.html'),
 ('p','Maven Surefire: https://maven.apache.org/surefire/maven-surefire-plugin/')])
]

styles=getSampleStyleSheet()
styles.add(ParagraphStyle(name='ReportTitle',fontName='Helvetica-Bold',fontSize=26,leading=31,textColor=colors.HexColor(NAVY),spaceAfter=20))
styles.add(ParagraphStyle(name='ReportBody',fontName='Helvetica',fontSize=10.5,leading=16,textColor=colors.HexColor(NAVY),spaceAfter=13))
styles.add(ParagraphStyle(name='ReportSub',fontName='Helvetica-Bold',fontSize=14,leading=19,textColor=colors.HexColor(TEAL),spaceBefore=9,spaceAfter=12))
styles.add(ParagraphStyle(name='Cell',fontName='Helvetica',fontSize=9.4,leading=14,textColor=colors.HexColor(NAVY)))
story=[];md=['# Train Ticket Reservation System\n\nCoursework project report\n']
for i,(title,items) in enumerate(pages):
    if i:story.append(PageBreak())
    if i==0:
        story.append(Paragraph('RAILWAY / COURSEWORK REPORT',styles['ReportSub']))
        story.append(Paragraph('Train Ticket<br/>Reservation System',styles['ReportTitle']))
        story.append(Paragraph('Java 17 + Swing + JDBC + MySQL',styles['ReportBody']))
    story.append(Paragraph(title,styles['ReportSub'] if i==0 else styles['ReportTitle']));md.append('## '+title+'\n')
    for kind,data in items:
        if kind in ('p','h'):
            story.append(Paragraph(escape(data),styles['ReportBody' if kind=='p' else 'ReportSub']));md.append(('### ' if kind=='h' else '')+data+'\n')
        elif kind=='image':
            story.append(RLImage(str(DOCS/data),width=483,height=322));story.append(Spacer(1,16));md.append('!['+data+']('+data+')\n')
        elif kind=='table':
            rows=[[Paragraph(escape(c),styles['Cell']) for c in row] for row in data]
            table=Table(rows,colWidths=[110,373],hAlign='LEFT')
            table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#DBEEEB')),('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.HexColor('#F4F7FA'),colors.white]),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),10),('RIGHTPADDING',(0,0),(-1,-1),10),('TOPPADDING',(0,0),(-1,-1),10),('BOTTOMPADDING',(0,0),(-1,-1),10)]))
            story.append(table);story.append(Spacer(1,18));md.extend([' | '.join(r)+'\n' for r in data])
def footer(canvas,doc):
    canvas.setStrokeColor(colors.HexColor(TEAL));canvas.line(56,800,539,800)
    canvas.setFont('Helvetica',8);canvas.setFillColor(colors.HexColor(MUTED))
    canvas.drawString(56,814,'RAILWAY / TRAIN TICKET RESERVATION SYSTEM')
    canvas.drawString(56,30,'COURSEWORK EDITION  -  SIMULATED PAYMENTS')
    canvas.drawRightString(539,30,str(doc.page))
SimpleDocTemplate(str(DOCS/'ProjectReport.pdf'),pagesize=A4,rightMargin=56,leftMargin=56,topMargin=62,bottomMargin=56,title='Railway - Train Ticket Reservation System',author='Coursework project documentation').build(story,onFirstPage=footer,onLaterPages=footer)
(DOCS/'ProjectReport.md').write_text('\n'.join(md),encoding='utf-8')
print('Generated diagrams and report in',DOCS)
