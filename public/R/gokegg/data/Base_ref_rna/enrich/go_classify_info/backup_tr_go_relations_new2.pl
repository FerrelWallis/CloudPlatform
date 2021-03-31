#!usr/bin/perl -w

use strict;

die "usage:perl $0 <gene_ontology.1_2.obo> <GO.list>\n" unless @ARGV == 2;

open A,$ARGV[0];
open B,$ARGV[1];

my %h1;my %tar;my %rela;my %h2;my %func;
$/="Term]\nid: ";
<A>;
while(<A>){
    chomp;my @a=split /\n+/;
    my @b=split /: /,$a[1]; #function
    my @c=split /: /,$a[2]; #main function
    #print "$a[0]\t$b[1]\t$c[1]\n";
    $func{$a[0]}=$c[1];
    foreach my $k1(3..$#a){
        my @dd=split /\s+/,$a[$k1];
        if($a[$k1]=~/is_a:/){
            $h1{$a[0]}{$dd[1]}="is";
        }
        if($a[$k1]=~/^relationship:/){
            $h2{$a[0]}{$dd[2]}=$dd[1];
        }
    }
}
close A;
$/="\n";

while(<B>){
    chomp;my @a=split;
    $tar{$a[0]}=$a[0];
    $rela{$a[0]}=$a[0];
    #print "$a[0]";
    foreach my $kk(1..10){
        foreach my $k2(sort keys %tar){
            if(exists $h1{$k2}){
                my $tmp_rela=$tar{$k2};
                foreach my $k4(sort keys %{$h1{$k2}}){
                    my $id=$tmp_rela." ".$k4;
                    $tar{$k4}=$id;
                    $rela{$id}=1;
                    delete $rela{$tmp_rela};
                    #print "\t$k4";
                }
                delete $tar{$k2};
            }
        }
    }
    #print "\n";
}
close B;

foreach my $k1(sort keys %rela){
    print "$k1\n";
}
