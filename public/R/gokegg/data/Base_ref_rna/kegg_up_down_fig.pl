#!/usr/bin/perl -w
use strict;
use warnings;
use Carp;
use Bio::SearchIO;
use Getopt::Long;
use DBI qw(:sql_types);
use SOAP::Lite;
use Try::Tiny;
use LWP::Simple;
use LWP::UserAgent;
use HTML::TreeBuilder;
use Math::Round qw(:all);
use GD;
sub SOAP::Serializer::as_StringArray {
	my ($self, $value, $name, $type, $attr) = @_;
	return [$name, {'xsi:type' => 'array', %$attr}, $value];
}
sub SOAP::Serializer::as_ArrayOfstring{
	my ($self, $value, $name, $type, $attr) = @_;
	return [$name, {'xsi:type' => 'array', %$attr}, $value];
}
sub SOAP::Serializer::as_ArrayOfint{
	my ($self, $value, $name, $type, $attr) = @_;
	return [$name, {'xsi:type' => 'array', %$attr}, $value];
}
my %opts;
my $VERSION="2.0";
GetOptions( \%opts,"i1=s", "i2=s", "format=s", "o=s", "org=s","remote=s","fresh!","rank=i","database=s","QminCoverage=i","server=i","parse_id!", "h!");

my $usage = <<"USAGE";
	Program : $0
	Version : $VERSION
	Discription:parse blast to genes databse result and get kegg pathway info and map
	please install perl model: bioperl DBI DBD::SQLite SOAP::Lite autodie Try::Tiny
	Usage :perl $0 [options]
	-i1		pathway1
	-i2	        pathway2
	-parse_id			parse_id from query description not the Query ID
	-o		dir			output dir,defualt kegg_out under current dir                 
	-org		organism		organism name of three letters ,list in http://www.genome.jp/kegg/catalog/org_list.html ,like hsa	defuat:ko,
	-fresh					fresh database from network
	-database	database path		defuat:/home/db/kegg/kegg.db
	-h					Display this usage information
USAGE
die $usage if ((!$opts{i1})||$opts{h}||!$opts{i2});
$opts{format}=$opts{format}?$opts{format}:"kobas";
$opts{o}=$opts{o}?$opts{o}:"./kegg_out";
$opts{org}=$opts{org}?$opts{org}:"ko";
$opts{server}=$opts{server}?$opts{server}:3;
$opts{server}=$opts{server}==2?2:1;
unless($opts{database}){
	$opts{database}="/lustre/sdb/zengl/bin/module/Anno/kegg.db";
}
my $local_img_dir = "/lustre/sdb/zengl/bin/module/Anno/kegg/";
my $remote = "no";
my $ua =LWP::UserAgent->new();
my $dbh = DBI->connect("dbi:SQLite:dbname=$opts{database}","root","majorbio",{AutoCommit => 1});
my $check=$dbh->prepare("select count(*) from sqlite_master where type='table' and name='pathway_".$opts{org}."'");
$check->execute();
my $service;		undef $service;
my $file1= glob $opts{i1};			my $file2= glob $opts{i2};
my $pathway = &getpathways($opts{org});		my $kos =&getpathwaykos($opts{org});
my %seqkos;
mkdir("$opts{o}","493") or die "Can't create dir at $opts{o}\n" unless( -e $opts{o});
open ANNOT1, "< $file1";	open ANNOT2, "< $file2";	<ANNOT1>;	<ANNOT2>;
while(<ANNOT1>){
	chomp;	next if(/^\s*#/);	last if(/^\/\/\/\//);
	if(/^(\S*)\s+(.*)$/){
		my $query_name=$1;		my $annot=$2;		next if $annot=~/^None/;	
		my @a=split(/\|/,$annot);				
		my $ko=$a[0];                  $seqkos{$query_name}{$ko}++;
		my $koid="ko:".$ko;
		if(exists($kos->{$koid})){
			foreach my $p (keys(%{$kos->{$koid}})){
				push(@{$pathway->{$p}{'kos1'}},$koid);	push(@{$pathway->{$p}{'seqs1'}},$query_name);					
			}
		}
	}
}
close ANNOT1;
while(<ANNOT2>){
        chomp;  next if(/^\s*#/);       last if(/^\/\/\/\//);
        if(/^(\S*)\s+(.*)$/){
                my $query_name=$1;                my $annot=$2;                next if $annot=~/^None/;
                my @a=split(/\|/,$annot);
                my $ko=$a[0];                   $seqkos{$query_name}{$ko}++;
                my $koid="ko:".$ko;
                if(exists($kos->{$koid})){
                        foreach my $p (keys(%{$kos->{$koid}})){
                                push(@{$pathway->{$p}{'kos2'}},$koid); push(@{$pathway->{$p}{'seqs2'}},$query_name);
                        }
                }
        }
}
close ANNOT2;
open(PATHWATY,"> $opts{o}/pathway_table.xls" ) || die "Can't open $opts{o}/pathway_table.xls\n";
print PATHWATY "PathWay\tPathway_definition\tseqs_kos_up\tseqs_kos_down\n";
foreach my $p (keys(%$pathway)){
	my $kolist1=&uniq(\@{$pathway->{$p}{'kos1'}});  my $kolist2=&uniq(\@{$pathway->{$p}{'kos2'}});
	next if(@$kolist1 < 1 && @$kolist2 < 1);
	my $imgname=&getimgname($p);	my $htmlfile=&getimgname1($p);	
	&Mark_Pathway_local($imgname, $htmlfile, $kolist1, $kolist2);
	my $seqlist1=&uniq(\@{$pathway->{$p}{'seqs1'}});	my $seqlist2=&uniq(\@{$pathway->{$p}{'seqs2'}});
	my $seq_ko_list1;	my $seq_ko_list2;
	foreach my $n (@$seqlist1){
		my @part=keys %{$seqkos{$n}};  
		$seq_ko_list1.=$n."(".join(",",@part).");";
	}
	foreach my $n (@$seqlist2){
		my @part=keys %{$seqkos{$n}}; 
	        $seq_ko_list2.=$n."(".join(",",@part).");";
	}
	if(!defined $seq_ko_list1){	$seq_ko_list1="_";	}
	if(!defined $seq_ko_list2){     $seq_ko_list2="_";      }
	print PATHWATY "$p\t".$pathway->{$p}{'definition'}."\t".$seq_ko_list1."\t".$seq_ko_list2."\n"; 
}
close PATHWATY;
sub Mark_Pathway_local(){
	my $img_name = shift;	my $html_name= shift;	
	my $ko_list1 = shift;	my $ko_list2 = shift;
	my $img_file = "";      my $html_file = "";
	my %ko_annot1;		my %ko_annot2;
	foreach(@$ko_list1){
		my $ko= $_;	$ko =~ s/ko://;        $ko_annot1{$ko}=1;
	}
	foreach(@$ko_list2){
	        my $ko= $_;     $ko =~ s/ko://;        $ko_annot2{$ko}=1;
	}
	if ($opts{org} eq "ko"){
		$img_file = $local_img_dir."/ko/".$img_name;         $html_file = $local_img_dir."/ko/".$html_name;
	}elsif($opts{org} eq "map"){
		$img_file = $local_img_dir."/map/".$img_name;        $html_file = $local_img_dir."/map/".$html_name;
	}
	next unless (-e $html_file);
	my $datapage=HTML::TreeBuilder->new_from_file($html_file);
	my @data=$datapage->find_by_attribute("shape","rect");		
	my $image = GD::Image->newFromPng($img_file);
	my $red = $image->colorAllocate(255,0,0);	my $yell= $image->colorAllocate(255,255,0);	my $gree= $image->colorAllocate(0,255,255);
	my $width = 2;					$image->setThickness($width);
	foreach(@data){
		my $position = $_ ->attr("coords"); my @coords = split(/,/,$position); my $href = $_ ->attr("href");
		my $isanno1 = 0;	my $isanno2=0;
		if($href =~ /http.*\?(.*)$/){
			my @ko_rect = split(/\+/,$1);
			foreach(@ko_rect){
				if(exists $ko_annot1{$_}){					$isanno1 = 1;				}
				if(exists $ko_annot2{$_}){				        $isanno2 = 1;				}
			}
		}
		if($isanno1 == 1 && $isanno2 == 0){			$image->rectangle($coords[0],$coords[1],$coords[2],$coords[3],$red);		}
		if($isanno1 == 0 && $isanno2 == 1){        		$image->rectangle($coords[0],$coords[1],$coords[2],$coords[3],$yell);		}
		if($isanno1 == 1 && $isanno2 == 1){		        $image->rectangle($coords[0],$coords[1],$coords[2],$coords[3],$gree);		}
	}
	open (PNG,"> $opts{o}/$img_name");
	binmode PNG;    print PNG $image->png;    close PNG;    system ("cp $html_file $opts{o}/$html_name");	
}
sub getimgname(){
	my $path=shift;
	my @a=split(":",$path);
	return $a[1].".png";
}
sub getimgname1(){
	my $path=shift;
	my @a=split(":",$path);
	return $a[1].".html";
}
sub getpathways(){
	my $org=shift;
	my $pw=$dbh->prepare(<<SQL
select class,definition from pathway_$org;
SQL
	);
	$pw->execute();
	my $ref = $pw->fetchall_hashref('class');
	$pw->finish;
	return $ref;
}
sub getpathwaykos(){
	my $org=shift;
	my %kolist;
	my $mm;
	my $pw=$dbh->prepare(<<SQL
select class,definition from pathway_$org;
SQL
	);
	if($opts{format} eq 'kobas' && $opts{org} ne 'ko' && $opts{org} ne 'map'){
		$mm=$dbh->prepare(<<SQL
select * from gene_pathway_$org;
SQL
	);
	}else{
	$mm=$dbh->prepare(<<SQL
select * from ko_pathway_$org;
SQL
	);
	}
	$mm->execute();
	my $ref = $mm->fetchall_hashref('id');
	foreach my $ids ( keys(%$ref) ) {
		my $k=$ref->{$ids}->{'ko'};
		my $p=$ref->{$ids}->{'pathway'};
		$kolist{$k}{$p}=1;
	}
	$mm->finish;
	return \%kolist;
}
sub uniq {
	my $array      = shift;
	my %hash       = map { $_ => 1 } @$array;
	my @uniq_array = sort( keys %hash );
	return \@uniq_array;
}

